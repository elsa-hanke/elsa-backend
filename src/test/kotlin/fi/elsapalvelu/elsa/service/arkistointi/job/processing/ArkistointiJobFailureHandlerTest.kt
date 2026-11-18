package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.Execution
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiRetryProperties
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.slf4j.MDC

class ArkistointiJobFailureHandlerTest {
    private val jobId = UUID.randomUUID()
    private val state = mock<ArkistointiJobStateService>()
    private val operations = mock<ExecutionOperations<Unit>>()
    private val policy = ArkistointiRetryPolicy(ArkistointiRetryProperties().apply {
        maxAttempts = 3
        jitterFactor = 0.0
    })
    private val jobs = mock<ArkistointiJobRepository>()
    private val logContext = ArkistointiJobLogContext(jobs)
    private val handler = ArkistointiJobFailureHandler(state, policy, logContext)

    init { whenever(state.requiresIntervention(any())).thenReturn(true) }

    @Test
    fun `scheduler counter determines delay and last allowed attempt terminates`() {
        val failure = failure(1)
        whenever(state.fail(any(), any(), eq(false))).thenReturn(true)
        whenever(state.fail(any(), any(), eq(true))).thenReturn(false)
        handler.onFailure(failure, operations)
        verify(operations).reschedule(failure, failure.timeDone.plus(Duration.ofMinutes(2)))

        handler.onFailure(failure(2), operations)
        verify(state).fail(ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), 1L), ArkistointiError.create(
            "ARCHIVE_RETRIES_EXHAUSTED",
            "Arkistoinnin enimmainen yritysmaara saavutettiin. ARCHIVE_PROCESSING_FAILED: Arkistoinnin kasittely epaonnistui"
        ), true)
        verify(operations).reschedule(any(), eq(Schedule.NEVER))
    }

    @Test
    fun `jittered delay is passed to the scheduler reschedule API`() {
        val jitteredPolicy = ArkistointiRetryPolicy { 0.5 }
        val completed = failure(1)
        whenever(state.fail(any(), any(), eq(false))).thenReturn(true)
        ArkistointiJobFailureHandler(state, jitteredPolicy, logContext).onFailure(completed, operations)
        verify(operations).reschedule(completed, completed.timeDone.plusSeconds(114))
        verify(operations, never()).remove()
    }

    @Test
    fun `failed state commit never acknowledges or reschedules the execution`() {
        whenever(state.fail(any(), any(), any())).thenThrow(IllegalStateException("db unavailable"))
        assertThrows<ArkistointiProcessingException> { handler.onFailure(failure(0), operations) }
        verify(operations, never()).remove()
        verify(operations, never()).reschedule(any(), any())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 2, 3])
    fun `permanent error takes priority over exhausted retry counter`(previousFailures: Int) {
        val error = ArkistointiError.create("REJECTED", "Aineisto hylattiin")
        val completed = failure(previousFailures, ArkistointiProcessingException(error, retryable = false))
        handler.onFailure(completed, operations)
        verify(state).fail(ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), 1L), error, true)
        verify(operations).reschedule(any(), eq(Schedule.NEVER))
    }

    @Test
    fun `failed permanent commit preserves classification and can be completed after database recovery`() {
        val error = ArkistointiError.create("REJECTED", "Aineisto hylattiin")
        val completed = failure(0, ArkistointiProcessingException(error, retryable = false))
        whenever(state.fail(any(), any(), eq(true))).thenThrow(IllegalStateException("private database detail")).thenReturn(false)
        val exception = assertThrows<ArkistointiProcessingException> { handler.onFailure(completed, operations) }
        assertThat(exception.error).isEqualTo(error)
        assertThat(exception.retryable).isFalse()
        assertThat(exception.cause).isNull()
        verifyNoInteractions(operations)
        handler.onFailure(completed, operations)
        verify(operations).reschedule(any(), eq(Schedule.NEVER))
        verify(operations, never()).remove()
    }

    @Test
    fun `stale execution does not remove or reschedule a newer execution`() {
        whenever(state.fail(any(), any(), any())).thenThrow(StaleArkistointiExecutionException())
        handler.onFailure(failure(0), operations)
        verifyNoInteractions(operations)
    }

    @ParameterizedTest
    @ValueSource(strings = ["42", "not-a-uuid", "1-1-1-1-1"])
    fun `malformed scheduler identity is removed without modifying a domain job`(id: String) {
        val now = Instant.now()
        val execution = Execution(now, ARKISTOINTI_JOB_TASK.instance(id).build())

        handler.onFailure(ExecutionComplete.failure(execution, now, now, IllegalArgumentException()), operations)

        verifyNoInteractions(state)
        verify(operations).remove()
        verify(operations, never()).reschedule(any(), any())
    }

    @Test
    fun `dead execution uses recovery state decision instead of normal retry`() {
        val completed = failure(0)
        whenever(state.recoverDeadExecution(any(), any(), eq(false))).thenReturn(false)
        handler.onDeadExecution(completed, operations)
        verify(state).recoverDeadExecution(any(), any(), eq(false))
        verify(state, never()).fail(any(), any(), any())
        verify(operations).reschedule(completed, Schedule.NEVER)
    }

    @Test
    fun `failure and dead execution handling run with the job id and requesting user in MDC`() {
        whenever(jobs.findRequestedByUserId(jobId)).thenReturn("user-9")
        val seen = mutableListOf<Pair<String?, String?>>()
        whenever(state.fail(any(), any(), any())).thenAnswer {
            seen.add(MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY))
            true
        }
        whenever(state.recoverDeadExecution(any(), any(), any())).thenAnswer {
            seen.add(MDC.get(MDC_USER_ID_KEY) to MDC.get(MDC_ARCHIVE_JOB_ID_KEY))
            true
        }

        handler.onFailure(failure(0), operations)
        handler.onDeadExecution(failure(0), operations)

        assertThat(seen).containsExactly("user-9" to jobId.toString(), "user-9" to jobId.toString())
        assertThat(MDC.get(MDC_USER_ID_KEY)).isNull()
        assertThat(MDC.get(MDC_ARCHIVE_JOB_ID_KEY)).isNull()
    }

    private fun failure(previousFailures: Int, cause: ArkistointiProcessingException = ArkistointiProcessingException(unexpectedArchiveError())): ExecutionComplete {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val execution = Execution(now, ARKISTOINTI_JOB_TASK.instance(jobId.toString()).build())
        execution.consecutiveFailures = previousFailures
        return ExecutionComplete.failure(execution, now, now, cause)
    }
}
