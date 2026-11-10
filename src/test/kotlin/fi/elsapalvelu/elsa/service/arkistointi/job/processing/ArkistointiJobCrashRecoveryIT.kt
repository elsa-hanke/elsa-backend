package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** Crashes, lost commits and dead-execution recovery: no scenario may replay a delivery with unknown outcome. */
class ArkistointiJobCrashRecoveryIT : ArkistointiJobProcessingItBase() {

    @Test
    fun `dead execution before processing resumes with the original idempotency key`() {
        val jobId = fixture()
        simulateCrash(jobId, processingStarted = false)
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions.single().key).isEqualTo(jobId)
    }

    @Test
    fun `dead execution after successful commit is acknowledged without another delivery`() {
        val jobId = fixture()
        simulateCrash(jobId)
        processor.process(execution(jobId))
        simulateCrash(jobId, completed = true)
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(1)
    }

    @Test
    fun `remote success followed by a failed local commit parks without replay`() {
        val jobId = fixture()
        installFailingTrigger("fail_archive_success", "arkistointi_job", "NEW.status = 'LAHETETTY'")
        val scheduler = scheduler()
        try {
            runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
        } finally {
            dropFailingTrigger("fail_archive_success", "arkistointi_job")
        }
        runUntil(scheduler) { assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA) }
        assertThat(taskCount(jobId)).isEqualTo(1)
        assertThat(adapter.submissions).hasSize(1)
        assertThat(adapter.submissions.map { it.key }).containsOnly(jobId)
    }

    @Test
    fun `failed retry reschedule is recovered after job state was committed`() {
        val jobId = fixture()
        val failOnce = AtomicBoolean(true)
        adapter.behavior = {
            if (failOnce.getAndSet(false)) {
                installFailingTrigger("fail_archive_reschedule", "scheduled_tasks", "OLD.picked AND NOT NEW.picked")
                retryable()
            } else ArkistointiResult.Success(null)
        }
        val scheduler = scheduler()
        try {
            runUntil(scheduler) {
                assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
                assertThat(isPicked(jobId)).isTrue()
                assertThat(scheduler.currentlyExecuting).isEmpty()
            }
        } finally {
            dropFailingTrigger("fail_archive_reschedule", "scheduled_tasks")
        }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(2)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PERMANENT", "INVALID_SUCCESS"])
    fun `lost uncommitted outcome parks without attempting another delivery`(result: String) {
        val expectedCode = if (result == "PERMANENT") "OLD_ERROR" else "ARCHIVE_ADAPTER_RESPONSE_INVALID"
        val jobId = fixture()
        val originalExecution = execution(jobId)
        val attemptCounters = CopyOnWriteArrayList<Int>()
        adapter.behavior = {
            attemptCounters.add(failures(jobId))
            lateResult(result)
        }
        val failedCommit = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val observingHandler = FailureHandler<Unit> { current, executionOperations ->
            try {
                failureHandler.onFailure(current, executionOperations)
            } catch (exception: Exception) {
                failure.set(exception)
                failedCommit.countDown()
                throw exception
            }
        }
        // Deferred trigger: the UPDATE/flush succeeds and COMMIT itself fails.
        installFailingTrigger("fail_archive_terminal", "arkistointi_job", "NEW.status = 'VAATII_TOIMENPITEITA'", deferred = true)
        val observedTask = object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, observingHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) = task.executeOnce(instance, context)
        }
        val scheduler = scheduler(observedTask, Duration.ofHours(1))
        try {
            scheduler.triggerCheckForDueExecutions()
            assertThat(failedCommit.await(10, TimeUnit.SECONDS)).isTrue()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(scheduler.currentlyExecuting).isEmpty() }
            val exception = failure.get() as ArkistointiProcessingException
            assertThat(exception.retryable).isFalse()
            assertThat(exception.error.code).isEqualTo(expectedCode)
            assertThat(exception.cause).isNull()
            assertUncommittedTerminalState(jobId)
        } finally {
            scheduler.stop()
            dropFailingTrigger("fail_archive_terminal", "arkistointi_job")
        }
        // Recovery must receive a fresh event from db-scheduler, without the uncommitted outcome.
        val recovered = CopyOnWriteArrayList<ExecutionComplete>()
        updateSchedulerRow(jobId, "last_heartbeat = ?", Timestamp.from(Instant.now().minusSeconds(60)))
        val recoveryScheduler = scheduler(observingRecoveryTask(recovered))
        runUntil(recoveryScheduler) { assertParked(jobId) }
        assertThat(recovered).hasSize(1)
        assertThat(recovered.single().cause).isEmpty()
        assertThat(storedJob(jobId).errorCode).isEqualTo("ARCHIVE_EXECUTION_LOST")
        assertThat(attemptCounters).containsExactly(0)
        assertThat(adapter.submissions.map { it.key }).containsOnly(jobId)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `stale recovery handler cannot change a newer running or successful job`(afterSuccess: Boolean) {
        val jobId = fixture()
        simulateCrash(jobId, processingStarted = false)
        val recovery = RecoveryGate()
        val deliveryEntered = CountDownLatch(1)
        val releaseDelivery = CountDownLatch(1)
        val recoveryTask = recovery.task(task, failureHandler)
        adapter.behavior = {
            deliveryEntered.countDown()
            check(releaseDelivery.await(10, TimeUnit.SECONDS))
            ArkistointiResult.Success("external-id")
        }
        val first = scheduler(recoveryTask)
        scheduler(recoveryTask)
        try {
            runUntil(first) { assertThat(deliveryEntered.count).isZero() }
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
            if (afterSuccess) {
                releaseDelivery.countDown()
                runUntil(first) { assertThat(taskCount(jobId)).isZero() }
            }
            recovery.releaseStale.countDown()
            assertThat(recovery.staleFinished.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(recovery.staleError.get()).isNull()
            assertThat(recovery.versions).hasSize(2)
            assertThat(recovery.versions.distinct()).hasSize(1)
            assertThat(status(jobId)).isEqualTo(
                if (afterSuccess) ArkistointiJobStatus.LAHETETTY else ArkistointiJobStatus.KASITTELYSSA
            )
        } finally {
            recovery.releaseStale.countDown()
            releaseDelivery.countDown()
        }
        runUntil(first) { assertThat(taskCount(jobId)).isZero() }
        assertSucceeded(jobId, "external-id")
        assertThat(adapter.submissions).hasSize(1)
    }

    private fun assertUncommittedTerminalState(jobId: UUID) {
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
        assertThat(storedJob(jobId).errorCode).isNull()
        assertThat(schedulerState(jobId)).containsEntry("picked", true)
        assertThat(failures(jobId)).isZero()
    }

    private fun observingRecoveryTask(recovered: MutableList<ExecutionComplete>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, failureHandler,
            { current, operations ->
                recovered.add(current)
                task.deadExecutionHandler.deadExecution(current, operations)
            }) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) = task.executeOnce(instance, context)
        }

    /** Two schedulers detect the same dead execution; the second (stale) handler is held back until released. */
    private class RecoveryGate {
        private val secondDetected = CountDownLatch(1)
        private val recovered = AtomicInteger()
        val releaseStale = CountDownLatch(1)
        val staleFinished = CountDownLatch(1)
        val staleError = AtomicReference<Throwable>()
        val versions = CopyOnWriteArrayList<Long>()

        fun task(delegate: OneTimeTask<Unit>, handler: ArkistointiJobFailureHandler): OneTimeTask<Unit> =
            Tasks.oneTime(ARKISTOINTI_JOB_TASK)
                .onFailure(handler)
                .onDeadExecution { completed, operations ->
                    versions.add(completed.execution.version)
                    if (recovered.getAndIncrement() == 0) {
                        // Both schedulers must read the same dead execution before either reschedules it.
                        check(secondDetected.await(10, TimeUnit.SECONDS))
                        handler.onDeadExecution(completed, operations)
                    } else {
                        secondDetected.countDown()
                        try {
                            check(releaseStale.await(10, TimeUnit.SECONDS))
                            handler.onDeadExecution(completed, operations)
                        } catch (exception: Throwable) {
                            staleError.set(exception)
                        } finally {
                            staleFinished.countDown()
                        }
                    }
                }
                .execute { instance, context -> delegate.executeOnce(instance, context) }
    }
}
