package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.slf4j.MDC

class ArkistointiJobLogContextTest {
    private val jobs = mock<ArkistointiJobRepository>()
    private val context = ArkistointiJobLogContext(jobs)
    private val jobId = UUID.randomUUID()

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `block sees requesting user and job id and both are cleared afterwards`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenReturn("user-1")
        var seen: Pair<String?, String?>? = null

        val result = context.withJob(jobId.toString()) {
            seen = MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY)
            "done"
        }

        assertThat(result).isEqualTo("done")
        assertThat(seen).isEqualTo("user-1" to jobId.toString())
        assertThat(MDC.get(MDC_USER_ID_KEY)).isNull()
        assertThat(MDC.get(MDC_ARCHIVE_JOB_ID_KEY)).isNull()
    }

    @Test
    fun `context is cleared when the block throws and the exception is unchanged`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenReturn("user-1")
        val failure = IllegalStateException("boom")

        val thrown = assertThrows<IllegalStateException> { context.withJob(jobId.toString()) { throw failure } }

        assertThat(thrown).isSameAs(failure)
        assertThat(MDC.get(MDC_USER_ID_KEY)).isNull()
        assertThat(MDC.get(MDC_ARCHIVE_JOB_ID_KEY)).isNull()
    }

    @Test
    fun `job without a requesting user only gets the job id`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenReturn(null)
        var seen: Pair<String?, String?>? = null

        context.withJob(jobId.toString()) { seen = MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY) }

        assertThat(seen).isEqualTo(null to jobId.toString())
    }

    @Test
    fun `failed user lookup never fails the block and still sets the job id`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenThrow(IllegalStateException("db unavailable"))
        var seen: Pair<String?, String?>? = null

        val result = context.withJob(jobId.toString()) {
            seen = MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY)
            42
        }

        assertThat(result).isEqualTo(42)
        assertThat(seen).isEqualTo(null to jobId.toString())
    }

    @Test
    fun `non UUID task instance runs the block without touching the database or MDC`() {
        var seen: Pair<String?, String?>? = null

        context.withJob("not-a-uuid") { seen = MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY) }

        assertThat(seen).isEqualTo(null to null)
        verifyNoInteractions(jobs)
    }

    @Test
    fun `previous MDC values are restored instead of removed`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenReturn("worker-user")
        MDC.put(MDC_USER_ID_KEY, "outer-user")

        context.withJob(jobId.toString()) { assertThat(MDC.get(MDC_USER_ID_KEY)).isEqualTo("worker-user") }

        assertThat(MDC.get(MDC_USER_ID_KEY)).isEqualTo("outer-user")
    }
}
