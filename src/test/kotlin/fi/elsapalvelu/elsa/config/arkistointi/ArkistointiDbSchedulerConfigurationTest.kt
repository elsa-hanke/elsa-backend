package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.task.Execution
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobFailureHandler
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.StaleArkistointiExecutionException
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.mock.env.MockEnvironment

class ArkistointiDbSchedulerConfigurationTest {
    private val processor = mock<ArkistointiJobProcessor>()
    private val provider = mock<ObjectProvider<ArkistointiJobProcessor>>().also {
        whenever(it.getIfAvailable()).thenReturn(processor)
    }
    private val task = ArkistointiDbSchedulerConfiguration().arkistointiJobTask(
        provider, mock<ArkistointiJobFailureHandler>(),
        ArkistointiAdapterRegistry(emptyList()), ArkistointiConfigurationProvider(ApplicationProperties()), MockEnvironment()
    )
    private val jobId = UUID.randomUUID()
    private val instance = ARKISTOINTI_JOB_TASK.instance(jobId.toString()).build()
    private val now = Instant.now()
    private val execution = Execution(now, instance)
    private val context = mock<ExecutionContext>().also { whenever(it.execution).thenReturn(execution) }
    private val identity = ArkistointiJobExecution(instance.taskName, instance.id, execution.version)
    private val operations = mock<ExecutionOperations<Unit>>()

    @Test
    fun `task passes the exact job UUID and captured execution identity and acknowledges normal completion`() {
        task.execute(instance, context).complete(ExecutionComplete.success(execution, now, now), operations)
        assertThat(identity.jobId).isEqualTo(jobId)
        verify(processor).process(identity)
        verify(operations).stop()
    }

    @Test
    fun `intervention completion parks instead of removing the scheduler task`() {
        doThrow(fi.elsapalvelu.elsa.service.arkistointi.job.processing.ParkArkistointiExecutionException())
            .whenever(processor).process(identity)
        val completed = ExecutionComplete.success(execution, now, now)
        task.execute(instance, context).complete(completed, operations)
        verify(operations).reschedule(completed, com.github.kagkarlsson.scheduler.task.schedule.Schedule.NEVER)
        org.mockito.kotlin.verify(operations, org.mockito.kotlin.never()).stop()
    }

    @Test
    fun `stale processor completion never acknowledges or reschedules a task`() {
        doThrow(StaleArkistointiExecutionException()).whenever(processor).process(identity)
        task.execute(instance, context).complete(ExecutionComplete.success(execution, now, now), operations)
        verifyNoInteractions(operations)
    }

    @Test
    fun `persistence error remains a failure rather than successful acknowledgement`() {
        doThrow(IllegalStateException("Database unavailable")).whenever(processor).process(identity)
        assertThrows<IllegalStateException> { task.execute(instance, context) }
        verifyNoInteractions(operations)
    }

    @ParameterizedTest
    @ValueSource(strings = ["123", "not-a-uuid", "1-1-1-1-1"])
    fun `invalid task IDs are rejected before invoking the processor`(id: String) {
        val exception = assertThrows<IllegalArgumentException> {
            task.executeOnce(ARKISTOINTI_JOB_TASK.instance(id).build(), context)
        }

        assertThat(exception.message).isEqualTo("Arkistointi-jobin tunniste ei ole UUID")
        verifyNoInteractions(processor)
    }
}
