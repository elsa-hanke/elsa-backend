package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import java.time.Instant
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution

/** Execute the actual operator script with psql, including variable quoting and ON_ERROR_STOP. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynastyResolveStepIT {
    private val ops = DynastyOpsDatabase()
    private val jdbc get() = ops.jdbc
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)

    @BeforeAll
    fun startDatabase() {
        ops.start()
    }

    @AfterAll
    fun stopDatabase() = ops.stop()

    private fun fixture(step: String = "CASE"): UUID {
        val execution = stepFixture.execution(
            jobStatus = ArkistointiJobStatus.VAATII_TOIMENPITEITA,
            picked = false, executionTime = Instant.parse("2500-01-01T12:00:00Z")
        )
        jdbc.update("insert into arkistointi_job_asiakirja values (?,0)", execution.jobId)
        stepFixture.startedStep(execution, step)
        return execution.jobId
    }

    private fun resolve(id: UUID, step: String = "CASE", remote: String = "415", action: String = "confirm") =
        ops.resolveStep(id, step, remote, action)

    @ParameterizedTest
    @CsvSource(value = ["CASE|415", "DOCUMENT:0|9223372036854775807", "TASK|ABCDEF12-3456-7890-ABCD-EF1234567890", "ARCHIVE_CASE|''"], delimiter = '|')
    fun `confirm stores exactly one immutable completed step`(step: String, remote: String) {
        val id = fixture(step)
        val result = resolve(id, step, remote)
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        val row = requireNotNull(stepFixture.step(id, step))
        assertThat(row.status).isEqualTo(DynastyStepStatus.VALMIS)
        assertThat(row.remoteId).isEqualTo(remote.ifEmpty { null })
        assertThat(row.completedAt).isNotNull()
        assertThat(resolve(id, step, remote).exitCode).isNotZero()
        assertThat(jdbc.queryForObject("select status from arkistointi_job where id=?", String::class.java, id))
            .isEqualTo("VAATII_TOIMENPITEITA")
    }

    @Test
    fun `discard removes only the selected unfinished step`() {
        val id = fixture()
        stepFixture.startedStep(ArkistointiJobExecution("arkistointi-job", id.toString(), 1), "TASK")
        val result = resolve(id, remote = "", action = "discard")
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        assertThat(stepFixture.steps(id).map { it.key })
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
        val intent = requireNotNull(stepFixture.step(id, step))
        assertThat(intent.status).isEqualTo(DynastyStepStatus.ALOITETTU)
        assertThat(intent.remoteId).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["picked=true", "execution_time=CURRENT_TIMESTAMP", "task_name='other-task'"])
    fun `missing active or nonparked scheduler task prevents resolution`(assignment: String) {
        val id = fixture()
        // Assignments are fixed test literals, never operator input.
        jdbc.update("update scheduled_tasks set $assignment where task_instance=?", id.toString())
        assertThat(resolve(id).exitCode).isNotZero()
        assertThat(stepFixture.step(id, "CASE")?.status).isEqualTo(DynastyStepStatus.ALOITETTU)
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
            assertThat(stepFixture.step(id, "CASE")?.status).isEqualTo(DynastyStepStatus.ALOITETTU)
        } finally {
            jdbc.execute("DROP TRIGGER suppress_resolution ON arkistointi_dynasty_step")
            jdbc.execute("DROP FUNCTION suppress_resolution()")
        }
    }
}
