package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import java.nio.file.Path
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

/** Execute the actual operator script with psql, including variable quoting and ON_ERROR_STOP. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynastyResolveStepIT {
    private val database = PostgreSQLContainer("postgres:16.9")
    private lateinit var jdbc: JdbcTemplate

    @BeforeAll
    fun startDatabase() {
        database.start()
        jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        jdbc.execute("create table scheduled_tasks(task_name text,task_instance text,version bigint,picked boolean," +
            "execution_time timestamptz,primary key(task_name,task_instance))")
        jdbc.execute("create table arkistointi_job(id uuid primary key,status text)")
        jdbc.execute("create table arkistointi_job_asiakirja(job_id uuid,jarjestys integer)")
        val migration = javaClass.getResourceAsStream("/config/liquibase/changelog/20261101090000_added_arkistointi_dynasty_step.xml")!!
        migration.use {
            jdbc.execute(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).getElementsByTagName("sql").item(0).textContent)
        }
        database.copyFileToContainer(
            MountableFile.forHostPath(Path.of("ops/arkistointi/dynasty-resolve-step.sql").toAbsolutePath().toString()),
            "/tmp/resolve.sql"
        )
    }

    @AfterAll
    fun stopDatabase() = database.stop()

    private fun fixture(step: String = "CASE"): UUID {
        val id = UUID.randomUUID()
        jdbc.update("insert into arkistointi_job values (?,'VAATII_TOIMENPITEITA')", id)
        jdbc.update("insert into scheduled_tasks values ('arkistointi-job',?,1,false,'2500-01-01 12:00:00+00')", id.toString())
        jdbc.update("insert into arkistointi_job_asiakirja values (?,0)", id)
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,?,'ALOITETTU',1,CURRENT_TIMESTAMP)", id, step)
        return id
    }

    private fun resolve(id: UUID, step: String = "CASE", remote: String = "415", action: String = "confirm") =
        database.execInContainer("psql", "-U", database.username, "-d", database.databaseName,
            "-v", "job_id=$id", "-v", "step_key=$step", "-v", "remote_id=$remote", "-v", "action=$action", "-f", "/tmp/resolve.sql")

    @ParameterizedTest
    @CsvSource(value = ["CASE|415", "DOCUMENT:0|9223372036854775807", "TASK|ABCDEF12-3456-7890-ABCD-EF1234567890", "ARCHIVE_CASE|''"], delimiter = '|')
    fun `confirm stores exactly one immutable completed step`(step: String, remote: String) {
        val id = fixture(step)
        val result = resolve(id, step, remote)
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        val row = jdbc.queryForMap("select status,remote_id,completed_at from arkistointi_dynasty_step where job_id=?", id)
        assertThat(row["status"]).isEqualTo("VALMIS")
        assertThat(row["remote_id"]).isEqualTo(remote.ifEmpty { null })
        assertThat(row["completed_at"]).isNotNull()
        assertThat(resolve(id, step, remote).exitCode).isNotZero()
        assertThat(jdbc.queryForObject("select status from arkistointi_job where id=?", String::class.java, id))
            .isEqualTo("VAATII_TOIMENPITEITA")
    }

    @Test
    fun `discard removes only the selected unfinished step`() {
        val id = fixture()
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,'TASK','ALOITETTU',1,CURRENT_TIMESTAMP)", id)
        val result = resolve(id, remote = "", action = "discard")
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        assertThat(jdbc.queryForList("select step_key from arkistointi_dynasty_step where job_id=?", String::class.java, id))
            .containsExactly("TASK")
        assertThat(resolve(id, remote = "", action = "discard").exitCode).isNotZero()
    }

    @ParameterizedTest
    @CsvSource(value = [
        "unknown|CASE|415", "confirm|UNKNOWN|415", "confirm|DOCUMENT:-1|415", "confirm|DOCUMENT:01|415",
        "confirm|TASK|1-1-1-1-1", "confirm|TASK|not-a-uuid", "confirm|CASE|0", "confirm|CASE|-1",
        "confirm|CASE|9223372036854775808", "confirm|CASE|1.5", "confirm|CASE|''", "confirm|DOCUMENT:0|text",
        "confirm|ARCHIVE_CASE|415", "discard|CASE|415"
    ], delimiter = '|')
    fun `invalid input fails without changing the intent`(action: String, step: String, remote: String) {
        val id = fixture(step)
        val result = resolve(id, step, remote, action)
        assertThat(result.exitCode).isNotZero()
        assertThat(result.stderr).contains("Dynasty resolve:")
        assertThat(jdbc.queryForMap("select status,remote_id from arkistointi_dynasty_step where job_id=?", id))
            .containsEntry("status", "ALOITETTU").containsEntry("remote_id", null)
    }

    @ParameterizedTest
    @ValueSource(strings = ["picked=true", "execution_time=CURRENT_TIMESTAMP", "task_name='other-task'"])
    fun `missing active or nonparked scheduler task prevents resolution`(assignment: String) {
        val id = fixture()
        // Assignments are fixed test literals, never operator input.
        jdbc.update("update scheduled_tasks set $assignment where task_instance=?", id.toString())
        assertThat(resolve(id).exitCode).isNotZero()
        assertThat(jdbc.queryForObject("select status from arkistointi_dynasty_step where job_id=?", String::class.java, id))
            .isEqualTo("ALOITETTU")
    }

    @Test
    fun `nonintervention job and document outside manifest are rejected`() {
        val id = fixture()
        jdbc.update("update arkistointi_job set status='ODOTTAA' where id=?", id)
        assertThat(resolve(id).stderr).contains("job must exist and require intervention")
        val outside = fixture("DOCUMENT:1")
        assertThat(resolve(outside, "DOCUMENT:1").stderr).contains("document step is not in the job manifest")
    }

    @Test
    fun `zero changed rows abort the transaction`() {
        val id = fixture()
        jdbc.execute("CREATE FUNCTION suppress_resolution() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RETURN NULL; END;'")
        jdbc.execute("CREATE TRIGGER suppress_resolution BEFORE UPDATE ON arkistointi_dynasty_step " +
            "FOR EACH ROW EXECUTE FUNCTION suppress_resolution()")
        try {
            val result = resolve(id)
            assertThat(result.exitCode).isNotZero()
            assertThat(result.stderr).contains("expected exactly one changed step, got 0")
            assertThat(jdbc.queryForObject("select status from arkistointi_dynasty_step where job_id=?", String::class.java, id))
                .isEqualTo("ALOITETTU")
        } finally {
            jdbc.execute("DROP TRIGGER suppress_resolution ON arkistointi_dynasty_step")
            jdbc.execute("DROP FUNCTION suppress_resolution()")
        }
    }
}
