package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.CompletionHandler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrors
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer

/** All SQL writes simulate recovery in the isolated disposable PostgreSQL test database. */
class ArkistointiLoadingRecoveryIT : ArkistointiJobProcessingItBase() {

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `heartbeat recovery parks paused loading and old attempt cannot send or overwrite recovery`(loadingFails: Boolean) {
        val loadingEntered = CountDownLatch(1)
        val releaseLoading = CountDownLatch(1)
        val loads = AtomicInteger()
        val versions = CopyOnWriteArrayList<Long>()
        doAnswer { invocation ->
            if (loads.getAndIncrement() == 0) {
                loadingEntered.countDown()
                check(releaseLoading.await(30, TimeUnit.SECONDS))
                if (loadingFails) throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
            }
            invocation.callRealMethod()
        }.`when`(loader).load(any())
        val jobId = fixture()
        val first = scheduler(observedTask(versions), Duration.ofHours(1))
        try {
            assertThat(loadingEntered.await(10, TimeUnit.SECONDS)).isTrue()
            // The previous implementation cannot acquire these locks until loading is released.
            // NOWAIT makes that regression fail deterministically rather than hanging the test.
            transactions.executeWithoutResult {
                jdbc.queryForMap("select version from scheduled_tasks where task_instance = ? for update nowait", jobId.toString())
                jdbc.queryForMap("select id from arkistointi_job where id = ? for update nowait", jobId)
                assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
                jdbc.update("update scheduled_tasks set last_heartbeat = clock_timestamp() - interval '60 seconds' " +
                    "where task_instance = ?", jobId.toString())
            }
            val second = scheduler(observedTask(versions))
            runUntil(second) {
                val job = jobs.findById(jobId).orElseThrow()
                assertThat(job.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
                assertThat(job.errorCode).isEqualTo("ARCHIVE_EXECUTION_LOST")
                val schedule = schedulerStateOrEmpty(jobId)
                assertThat(schedule["picked"]).isEqualTo(false)
                assertThat((schedule["execution_time"] as java.sql.Timestamp).toInstant()).isEqualTo(Schedule.NEVER)
            }
            assertThat(versions).hasSize(1)
            assertThat(loads.get()).isEqualTo(1)
            assertThat(adapter.submissions).isEmpty()
            val beforeSchedule = schedulerStateOrEmpty(jobId)
            val beforeJob = jobState(jobId)
            releaseLoading.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(adapter.submissions).isEmpty()
            assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(beforeSchedule)
            assertThat(jobState(jobId)).isEqualTo(beforeJob)
            assertThat(versions).hasSize(1)
            assertThat(loads.get()).isEqualTo(1)
        } finally {
            releaseLoading.countDown()
            schedulers.forEach(Scheduler::stop)
        }
    }

    @Test
    fun `attempt does not acknowledge recovery pending reschedule with the same version`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        doAnswer { invocation ->
            entered.countDown()
            check(release.await(20, TimeUnit.SECONDS))
            invocation.callRealMethod()
        }.`when`(loader).load(any())
        val jobId = fixture()
        val versions = CopyOnWriteArrayList<Long>()
        val first = scheduler(observedTask(versions), Duration.ofHours(1))
        try {
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue()
            val execution = ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), versions.single())
            assertThat(state.fail(execution, ArkistointiError.create("RECOVERING", "Uudelleenyritys"), stopRetrying = false)).isTrue()
            val before = schedulerStateOrEmpty(jobId)
            release.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(adapter.submissions).isEmpty()
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(before)
        } finally {
            release.countDown()
            first.stop()
        }
    }

    private fun observedTask(versions: MutableList<Long>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, task.failureHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) =
                task.executeOnce(instance, context)

            override fun execute(instance: TaskInstance<Unit>, context: ExecutionContext): CompletionHandler<Unit> {
                versions.add(context.execution.version)
                return task.execute(instance, context)
            }
        }
}
