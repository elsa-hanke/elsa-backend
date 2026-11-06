package fi.elsapalvelu.elsa.service.arkistointi.job.processing

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

/** Exercise the operator's actual psql script, including failure exit codes and transaction rollback. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchiveRetryScriptIT {
    private val database = PostgreSQLContainer("postgres:16.9")
    private lateinit var jdbc: JdbcTemplate

    @BeforeAll
    fun startDatabase() {
        database.start()
        jdbc = JdbcTemplate(DriverManagerDataSource(database.jdbcUrl, database.username, database.password))
        jdbc.execute("create table scheduled_tasks(task_name text,task_instance text,version bigint,picked boolean," +
            "execution_time timestamptz,picked_by text,last_heartbeat timestamptz,consecutive_failures integer," +
            "last_failure timestamptz,last_success timestamptz,primary key(task_name,task_instance))")
        jdbc.execute("create table arkistointi_job(id uuid primary key,status text,completed_at timestamptz," +
            "updated_at timestamptz,error_code text,last_error text,payload text)")
        database.copyFileToContainer(
            MountableFile.forHostPath(Path.of("ops/arkistointi/retry-job.sql").toAbsolutePath().toString()), "/tmp/retry.sql"
        )
    }

    @AfterAll
    fun stopDatabase() = database.stop()

    private fun fixture(): UUID {
        val id = UUID.randomUUID()
        jdbc.update("insert into arkistointi_job values (?,'VAATII_TOIMENPITEITA',now(),now(),'ERROR','last failure','frozen')", id)
        jdbc.update("insert into scheduled_tasks values ('arkistointi-job',?,7,false,'2500-01-01 12:00:00+00'," +
            "'old-worker',now(),5,now(),now())", id.toString())
        return id
    }

    private fun retry(id: String) = database.execInContainer(
        "psql", "-U", database.username, "-d", database.databaseName, "-v", "job_id=$id", "-f", "/tmp/retry.sql"
    )

    @Test
    fun `parked job is rescheduled retaining last error and immutable payload`() {
        val id = fixture()
        val result = retry(id.toString())
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        assertThat(result.stdout).contains("retry scheduled")
        assertThat(jdbc.queryForMap("select status,completed_at,error_code,last_error,payload from arkistointi_job where id=?", id))
            .containsEntry("status", "ODOTTAA").containsEntry("completed_at", null)
            .containsEntry("error_code", "ERROR").containsEntry("last_error", "last failure").containsEntry("payload", "frozen")
        assertThat(jdbc.queryForMap("select version,picked,picked_by,last_heartbeat,consecutive_failures,last_failure,last_success " +
            "from scheduled_tasks where task_instance=?", id.toString()))
            .containsEntry("version", 8L).containsEntry("picked", false).containsEntry("picked_by", null)
            .containsEntry("last_heartbeat", null).containsEntry("consecutive_failures", 0)
            .containsEntry("last_failure", null).containsEntry("last_success", null)
        assertThat(jdbc.queryForObject("select execution_time <= clock_timestamp() from scheduled_tasks where task_instance=?",
            Boolean::class.java, id.toString())).isTrue()
        assertThat(retry(id.toString()).stderr).contains("not parked")
    }

    @ParameterizedTest
    @CsvSource(value = [
        "delete from scheduled_tasks where task_instance=?|scheduler task is missing",
        "update scheduled_tasks set task_name='other' where task_instance=?|scheduler task is missing",
        "update scheduled_tasks set picked=true where task_instance=?|scheduler task is active",
        "update scheduled_tasks set execution_time=now() where task_instance=?|not parked"
    ], delimiter = '|')
    fun `ineligible scheduler task fails without changing the job`(sql: String, reason: String) {
        val id = fixture()
        jdbc.update(sql, id.toString())
        val result = retry(id.toString())
        assertThat(result.exitCode).isNotZero()
        assertThat(result.stderr).contains(reason)
        assertThat(jdbc.queryForObject("select status from arkistointi_job where id=?", String::class.java, id))
            .isEqualTo("VAATII_TOIMENPITEITA")
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks where task_instance=?", Long::class.java, id.toString()))
            .isEqualTo(if (sql.startsWith("delete")) 0L else 1L)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ODOTTAA", "KASITTELYSSA", "LAHETETTY", "missing"])
    fun `missing or nonintervention job leaves scheduler untouched`(status: String) {
        val id = fixture()
        if (status == "missing") jdbc.update("delete from arkistointi_job where id=?", id)
        else jdbc.update("update arkistointi_job set status=? where id=?", status, id)
        val result = retry(id.toString())
        assertThat(result.exitCode).isNotZero()
        assertThat(result.stderr).contains(if (status == "missing") "archive job is missing" else "actual status: $status")
        assertThat(jdbc.queryForObject("select version from scheduled_tasks where task_instance=?", Long::class.java, id.toString()))
            .isEqualTo(7L)
    }

    @Test
    fun `invalid UUID is rejected`() {
        assertThat(retry("not-a-uuid").exitCode).isNotZero()
    }

    @ParameterizedTest
    @ValueSource(strings = ["scheduled_tasks", "arkistointi_job"])
    fun `zero changed rows rolls back both updates`(table: String) {
        val id = fixture()
        jdbc.execute("CREATE FUNCTION suppress_retry() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RETURN NULL; END;'")
        jdbc.execute("CREATE TRIGGER suppress_retry BEFORE UPDATE ON $table FOR EACH ROW EXECUTE FUNCTION suppress_retry()")
        try {
            val result = retry(id.toString())
            assertThat(result.exitCode).isNotZero()
            assertThat(result.stderr).contains("expected exactly one changed")
            assertThat(jdbc.queryForObject("select status from arkistointi_job where id=?", String::class.java, id))
                .isEqualTo("VAATII_TOIMENPITEITA")
            assertThat(jdbc.queryForObject("select version from scheduled_tasks where task_instance=?", Long::class.java, id.toString()))
                .isEqualTo(7L)
        } finally {
            jdbc.execute("DROP TRIGGER suppress_retry ON $table")
            jdbc.execute("DROP FUNCTION suppress_retry()")
        }
    }
}
