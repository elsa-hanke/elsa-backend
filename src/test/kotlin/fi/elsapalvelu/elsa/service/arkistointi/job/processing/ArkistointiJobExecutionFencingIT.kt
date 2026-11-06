package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.task.CompletionHandler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.verifyNoInteractions
import org.junit.jupiter.api.Test

/** Version fencing and row locking: a stale or late actor must never overwrite the state of a newer execution. */
class ArkistointiJobExecutionFencingIT : ArkistointiJobProcessingItBase() {

    @ParameterizedTest
    @ValueSource(strings = ["UNPICKED", "REPLACED", "ACKNOWLEDGED"])
    fun `stale prepare never loads or sends documents`(reason: String) {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        when (reason) {
            "REPLACED" -> updateSchedulerRow(jobId, "picked = true, version = version + 1")
            "ACKNOWLEDGED" -> transactions.executeWithoutResult {
                jdbc.update("delete from scheduled_tasks where task_instance = ?", jobId.toString())
            }
        }
        val stored = schedulerStateOrEmpty(jobId)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        verifyNoInteractions(loader)
        assertThat(adapter.submissions).isEmpty()
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(stored)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `state success validates response defensively after checking execution ownership`(stale: Boolean) {
        val jobId = fixture()
        simulateCrash(jobId)
        val originalExecution = execution(jobId)
        if (stale) updateSchedulerRow(jobId, "version = version + 1")
        val beforeJob = jobState(jobId)
        val beforeSchedule = schedulerState(jobId)
        if (stale) {
            assertThrows<StaleArkistointiExecutionException> { state.succeed(originalExecution, "x".repeat(256)) }
        } else {
            val exception = assertThrows<ArkistointiProcessingException> { state.succeed(originalExecution, "x".repeat(256)) }
            assertThat(exception.error.code).isEqualTo("ARCHIVE_ADAPTER_RESPONSE_INVALID")
            assertThat(exception.retryable).isFalse()
        }
        assertThat(jobState(jobId)).isEqualTo(beforeJob)
        assertThat(schedulerState(jobId)).isEqualTo(beforeSchedule)
        assertThat(adapter.submissions).isEmpty()
    }

    @Test
    fun `concurrent state writer rechecks terminal status after waiting for a row lock`() {
        val jobId = fixture()
        updateSchedulerRow(jobId, "picked = true")
        val currentExecution = execution(jobId)
        val executor = Executors.newSingleThreadExecutor()
        lateinit var lateFailure: Future<Boolean>
        try {
            transactions.executeWithoutResult {
                val job = jobs.findByIdForStateChange(jobId)!!
                val lockOwner = jdbc.queryForObject("select pg_backend_pid()", Int::class.javaObjectType)!!
                lateFailure = executor.submit<Boolean> {
                    state.fail(currentExecution, ArkistointiError.create("LATE_ERROR", "Viivastynyt virhe"), false)
                }
                // Wait for an actual database lock wait, rather than relying on thread timing.
                await().atMost(Duration.ofSeconds(10)).until {
                    jdbc.queryForObject(
                        "select count(*) from pg_locks where not granted and ? = any(pg_blocking_pids(pid))",
                        Long::class.javaObjectType, lockOwner
                    )!! > 0
                }
                job.transitionTo(ArkistointiJobStatus.KASITTELYSSA)
                job.transitionTo(ArkistointiJobStatus.LAHETETTY)
                job.externalId = "committed-success"
                job.completedAt = Instant.now()
            }
            assertThat(lateFailure.get(10, TimeUnit.SECONDS)).isFalse()
            assertSucceeded(jobId, "committed-success")
        } finally {
            executor.shutdownNow()
        }
    }

    @ParameterizedTest
    @CsvSource("PERMANENT,false", "SUCCESS,false", "INVALID_SUCCESS,false", "RETRYABLE,false", "EXCEPTION,false",
        "PERMANENT,true", "SUCCESS,true", "INVALID_SUCCESS,true", "RETRYABLE,true", "EXCEPTION,true")
    fun `late adapter result cannot overwrite an explicitly replaced execution`(result: String, afterSuccess: Boolean) {
        val firstEntered = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val releaseSecond = CountDownLatch(1)
        val calls = AtomicInteger()
        val versions = CopyOnWriteArrayList<Long>()
        adapter.behavior = {
            if (calls.getAndIncrement() == 0) {
                firstEntered.countDown()
                check(releaseFirst.await(20, TimeUnit.SECONDS))
                lateResult(result)
            } else {
                secondEntered.countDown()
                check(releaseSecond.await(20, TimeUnit.SECONDS))
                ArkistointiResult.Success("new-external-id")
            }
        }
        val jobId = fixture()
        val first = scheduler(versionRecordingTask(versions), Duration.ofHours(1))
        try {
            assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue()
            // Isolate version fencing from dead-execution policy by explicitly replacing this test execution.
            // Production heartbeat recovery now parks it instead of authorizing a concurrent send.
            transactions.executeWithoutResult {
                jobs.findById(jobId).orElseThrow().transitionTo(ArkistointiJobStatus.ODOTTAA)
                jdbc.update("update scheduled_tasks set picked=false,picked_by=null,last_heartbeat=null,version=version+1 " +
                    "where task_instance=?", jobId.toString())
            }
            val second = scheduler(versionRecordingTask(versions))
            runUntil(second) { assertThat(secondEntered.count).isZero() }
            assertThat(versions).hasSize(2)
            assertThat(versions.last()).isGreaterThan(versions.first())
            val current = schedulerState(jobId)
            val currentJob = jobState(jobId)
            if (afterSuccess) {
                releaseSecond.countDown()
                runUntil(second) { assertThat(taskCount(jobId)).isZero() }
            }
            releaseFirst.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(status(jobId)).isEqualTo(
                if (afterSuccess) ArkistointiJobStatus.LAHETETTY else ArkistointiJobStatus.KASITTELYSSA
            )
            if (!afterSuccess) {
                assertThat(schedulerState(jobId)).isEqualTo(current)
                assertThat(jobState(jobId)).isEqualTo(currentJob)
            }
            releaseSecond.countDown()
            runUntil(second) { assertThat(taskCount(jobId)).isZero() }
            assertSucceeded(jobId, "new-external-id")
            assertThat(adapter.submissions).hasSize(2)
            assertThat(adapter.submissions.map { it.key }).containsOnly(jobId)
        } finally {
            releaseFirst.countDown()
            releaseSecond.countDown()
        }
    }

    private fun versionRecordingTask(versions: MutableList<Long>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java,
            failureHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) =
                task.executeOnce(instance, context)

            override fun execute(instance: TaskInstance<Unit>, context: ExecutionContext): CompletionHandler<Unit> {
                versions.add(context.execution.version)
                return task.execute(instance, context)
            }
        }
}
