package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider

class ArkistointiDbSchedulerConfigurationTest {
    private val processor = mock<ArkistointiJobProcessor>()
    private val provider = mock<ObjectProvider<ArkistointiJobProcessor>>()
    private val task = ArkistointiDbSchedulerConfiguration().arkistointiJobTask(provider)

    @Test
    fun `scheduler converts the task instance string to the exact job UUID`() {
        val id = UUID.randomUUID()
        whenever(provider.getIfAvailable()).thenReturn(processor)

        task.executeOnce(ARKISTOINTI_JOB_TASK.instance(id.toString()).build(), mock<ExecutionContext>())

        verify(processor).process(id)
    }

    @ParameterizedTest
    @ValueSource(strings = ["123", "not-a-uuid", "1-1-1-1-1"])
    fun `invalid task IDs are rejected before invoking the processor`(id: String) {
        val exception = assertThrows<IllegalArgumentException> {
            task.executeOnce(ARKISTOINTI_JOB_TASK.instance(id).build(), mock<ExecutionContext>())
        }

        assertThat(exception.message).isEqualTo("Arkistointi-jobin tunniste ei ole UUID")
        verifyNoInteractions(provider, processor)
    }
}
