package fi.elsapalvelu.elsa.service.arkistointi.job

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.FailureHandler
import org.springframework.stereotype.Component

@Component
class ArkistointiJobFailureHandler(
    private val state: ArkistointiJobStateService,
    private val policy: ArkistointiRetryPolicy
) : FailureHandler<Unit> {
    override fun onFailure(completed: ExecutionComplete, operations: ExecutionOperations<Unit>) {
        val execution = completed.execution
        val jobId = execution.taskInstance.id.toLongOrNull()
        if (jobId == null) {
            operations.remove()
            return
        }
        val exhausted = execution.consecutiveFailures >= policy.maxAttempts - 1
        val cause = completed.cause.orElse(null) as? ArkistointiProcessingException
        val lastError = cause?.error ?: unexpectedArchiveError()
        val error = if (exhausted) {
            ArkistointiError.create(
                "ARCHIVE_RETRIES_EXHAUSTED",
                "Arkistoinnin enimmainen yritysmaara saavutettiin. ${lastError.code}: ${lastError.description}"
            )
        } else {
            lastError
        }
        // If this commit fails, do not acknowledge the scheduler row. Dead-execution recovery retries it.
        val retry = try {
            state.failExecution(
                jobId, execution.taskInstance.taskName, execution.version, error,
                terminal = exhausted || cause?.retryable == false
            )
        } catch (_: Exception) {
            kotlin.error("Archive job state could not be persisted")
        } ?: return
        if (retry) {
            operations.reschedule(completed, completed.timeDone.plus(policy.delay(execution.consecutiveFailures)))
        } else {
            operations.remove()
        }
    }
}
