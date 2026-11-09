package fi.elsapalvelu.elsa.service.arkistointi.support

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.event.AbstractSchedulerListener
import com.github.kagkarlsson.scheduler.event.SchedulerListener.SchedulerEventType
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import org.awaitility.Awaitility.await
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Optional processor gate keeps receiver setup ahead of background delivery. */
class ArchiveSchedulerTestControl(private val scheduler: Scheduler, processor: ArkistointiJobProcessorImpl? = null) {
    private val polls = AtomicInteger()
    private val permits = Semaphore(0)

    init {
        scheduler.registerSchedulerListener(object : AbstractSchedulerListener() {
            override fun onSchedulerEvent(type: SchedulerEventType) {
                if (type == SchedulerEventType.RAN_EXECUTE_DUE) polls.incrementAndGet()
            }
        })
        if (processor != null) {
            doAnswer { invocation ->
                check(permits.tryAcquire(30, TimeUnit.SECONDS)) { "Test did not authorize archive attempt" }
                invocation.callRealMethod()
            }.whenever(processor).process(any<ArkistointiJobExecution>())
        }
    }

    fun startAttempt() {
        permits.release()
        scheduler.triggerCheckForDueExecutions()
    }

    fun pollAndAwait() {
        val before = polls.get()
        scheduler.triggerCheckForDueExecutions()
        await().atMost(Duration.ofSeconds(30)).until { polls.get() > before }
    }

    fun releasePendingAttempts() = permits.release(10)
}
