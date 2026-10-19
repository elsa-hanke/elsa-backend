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
        if (execution.taskInstance.id.toLongOrNull() == null) {
            operations.remove()
            return
        }
        val exhausted = execution.consecutiveFailures >= policy.maxAttempts - 1
        val cause = completed.cause.orElse(null) as? ArkistointiProcessingException
        val lastError = cause?.error ?: unexpectedArchiveError()
        val terminal = exhausted || cause?.retryable == false
        val error = if (exhausted && cause?.retryable != false) {
            ArkistointiError.create(
                "ARCHIVE_RETRIES_EXHAUSTED",
                "Arkistoinnin enimmainen yritysmaara saavutettiin. ${lastError.code}: ${lastError.description}"
            )
        } else {
            lastError
        }
        // If this commit fails, do not acknowledge the scheduler row. Dead-execution recovery retries it.
        val retry = try {
            state.fail(
                ArkistointiJobExecution(execution.taskInstance.taskName, execution.taskInstance.id, execution.version), error,
                terminal = terminal
            )
        } catch (_: StaleArkistointiExecutionException) {
            return
        } catch (_: Exception) {
            // Preserve this live execution's sanitized outcome; never acknowledge a failed commit.
            // Dead-execution recovery cannot reconstruct an outcome that was never committed.
            throw ArkistointiProcessingException(error, retryable = !terminal)
        }
        if (retry) {
            operations.reschedule(completed, completed.timeDone.plus(policy.delay(execution.consecutiveFailures)))
        } else {
            operations.remove()
        }
    }
}
