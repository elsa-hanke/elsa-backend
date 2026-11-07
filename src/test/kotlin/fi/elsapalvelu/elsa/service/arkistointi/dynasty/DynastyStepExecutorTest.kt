package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory

class DynastyStepExecutorTest {
    private val ledger = mock<DynastyStepLedger>()
    private val executor = DynastyStepExecutor(ledger, dynastyTestProperties("http://127.0.0.1"))
    private val jobId = UUID.randomUUID()
    private val execution = ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), 1L)
    private val logger = LoggerFactory.getLogger(DynastyStepExecutor::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun captureLogs() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun stopCapturing() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `executed step logs job id step and remote id`() {
        whenever(ledger.begin(eq(execution), any(), any())).thenReturn(DynastyStepLedger.Decision.Proceed)
        whenever(ledger.complete(any(), any(), any(), anyOrNull())).thenReturn(true)

        executor.execute(execution, DynastyStepKey.Case) { DynastyCaseId("415") }

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.INFO)
        assertThat(event.formattedMessage).isEqualTo("Dynasty step executed [jobId=$jobId, step=CASE, remoteId=415]")
    }

    @Test
    fun `step taken from the ledger is logged as reused and is not sent again`() {
        whenever(ledger.begin(eq(execution), any(), any())).thenReturn(DynastyStepLedger.Decision.Done("415"))
        var sent = false

        executor.execute(execution, DynastyStepKey.Case) { sent = true; DynastyCaseId("999") }

        assertThat(sent).isFalse()
        assertThat(appender.list.single().formattedMessage)
            .isEqualTo("Dynasty step reused from ledger [jobId=$jobId, step=CASE, remoteId=415]")
    }

    @Test
    fun `step in progress is logged as a warning with its category`() {
        whenever(ledger.begin(eq(execution), any(), any())).thenReturn(DynastyStepLedger.Decision.InProgress)

        assertThrows<DynastyFailure> { executor.execute(execution, DynastyStepKey.Document(2)) { DynastyDocumentId("1") } }

        val event = appender.list.single()
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.formattedMessage)
            .isEqualTo("Dynasty step failed [jobId=$jobId, step=DOCUMENT:2, category=STEP_IN_PROGRESS]")
    }

    @Test
    fun `failed send is logged and rethrown unchanged`() {
        whenever(ledger.begin(eq(execution), any(), any())).thenReturn(DynastyStepLedger.Decision.Proceed)
        val failure = DynastyFailure(DynastyFailureCategory.CONNECTION_FAILED)

        val thrown = assertThrows<DynastyFailure> { executor.execute(execution, DynastyStepKey.Task) { throw failure } }

        assertThat(thrown).isSameAs(failure)
        verify(ledger).abandon(any(), any(), any())
        verify(ledger, never()).complete(any(), any(), any(), anyOrNull())
        assertThat(appender.list.single().formattedMessage)
            .isEqualTo("Dynasty step failed [jobId=$jobId, step=TASK, category=CONNECTION_FAILED]")
    }
}
