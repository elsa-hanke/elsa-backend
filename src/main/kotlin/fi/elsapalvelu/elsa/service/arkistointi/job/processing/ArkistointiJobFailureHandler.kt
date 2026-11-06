package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import org.springframework.stereotype.Component

@Component
class ArkistointiJobFailureHandler(
    private val state: ArkistointiJobStateService,
    private val policy: ArkistointiRetryPolicy
) : FailureHandler<Unit> {
    override fun onFailure(completed: ExecutionComplete, operations: ExecutionOperations<Unit>) =
        handle(completed, operations, deadExecution = false)

    fun onDeadExecution(completed: ExecutionComplete, operations: ExecutionOperations<Unit>) =
        handle(completed, operations, deadExecution = true)

    private fun handle(completed: ExecutionComplete, operations: ExecutionOperations<Unit>, deadExecution: Boolean) {
        val execution = completed.execution
        val identity = try {
            ArkistointiJobExecution(execution.taskInstance.taskName, execution.taskInstance.id, execution.version)
        } catch (_: IllegalArgumentException) {
            operations.remove()
            return
        }
        val decision = failureDecision(completed)
        val retry = try {
            recordFailure(identity, decision, deadExecution)
        } catch (_: StaleArkistointiExecutionException) {
            return
        }
        if (retry) {
            operations.reschedule(completed, completed.timeDone.plus(policy.delay(execution.consecutiveFailures)))
        } else {
            parkOrRemove(identity, completed, operations)
        }
    }

    private data class FailureDecision(val error: ArkistointiError, val stopRetrying: Boolean)

    private fun failureDecision(completed: ExecutionComplete): FailureDecision {
        val exhausted = completed.execution.consecutiveFailures >= policy.maxAttempts - 1
        val cause = completed.cause.orElse(null) as? ArkistointiProcessingException
        val lastError = cause?.error ?: unexpectedArchiveError()
        val stopRetrying = exhausted || cause?.retryable == false
        val error = if (exhausted && cause?.retryable != false) {
            ArkistointiError.create(
                "ARCHIVE_RETRIES_EXHAUSTED",
                "Arkistoinnin enimmainen yritysmaara saavutettiin. ${lastError.code}: ${lastError.description}"
            )
        } else {
            lastError
        }
        return FailureDecision(error, stopRetrying)
    }

    private fun recordFailure(identity: ArkistointiJobExecution, decision: FailureDecision, deadExecution: Boolean): Boolean {
        // If this commit fails, do not acknowledge the scheduler row. Dead-execution recovery reconciles or parks it.
        return try {
            if (deadExecution) {
                state.recoverDeadExecution(identity, decision.error, decision.stopRetrying)
            }
            else {
                state.fail(identity, decision.error, decision.stopRetrying)
            }
        } catch (exception: StaleArkistointiExecutionException) {
            throw exception
        } catch (_: Exception) {
            // Preserve this live execution's sanitized outcome; never acknowledge a failed commit.
            // Dead-execution recovery cannot reconstruct an outcome that was never committed.
            throw ArkistointiProcessingException(decision.error, retryable = !decision.stopRetrying)
        }
    }

    private fun parkOrRemove(
        identity: ArkistointiJobExecution, completed: ExecutionComplete, operations: ExecutionOperations<Unit>
    ) {
        try {
            if (state.requiresIntervention(identity)) {
                operations.reschedule(completed, Schedule.NEVER)
            } else {
                operations.remove()
            }
        } catch (_: StaleArkistointiExecutionException) {
            // A newer execution owns the row.
        }
    }
}
