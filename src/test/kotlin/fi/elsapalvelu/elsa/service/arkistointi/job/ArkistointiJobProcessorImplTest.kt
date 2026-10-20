package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ArkistointiJobProcessorImplTest {
    private val state = mock<ArkistointiJobStateService>()
    private val adapter = mock<ArkistointiAdapter>().apply {
        whenever(university).thenReturn(YliopistoEnum.TURUN_YLIOPISTO)
    }
    private val processor = ArkistointiJobProcessorImpl(state, listOf(adapter))
    private val jobId = UUID.randomUUID()
    private val execution = ArkistointiJobExecution("arkistointi-job", jobId.toString(), 1L)
    private val submission = ArkistointiSubmission(jobId, YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", emptyList())

    @BeforeEach
    fun prepareSubmission() {
        whenever(state.prepare(execution)).thenReturn(submission)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NULL", "MAX_ASCII", "MAX_UNICODE", "OPAQUE"])
    fun `valid external ID is passed to persistence without modification`(kind: String) {
        val externalId = ArkistointiExternalIdTestData.value(kind)
        whenever(adapter.arkistoi(submission)).thenReturn(ArkistointiResult.Success(externalId))
        processor.process(execution)
        verify(state).succeed(execution, externalId)
    }

    @ParameterizedTest
    @ValueSource(strings = ["TOO_LONG", "TOO_LONG_UNICODE", "EMPTY", "BLANK", "NUL", "HIGH_SURROGATE", "LOW_SURROGATE"])
    fun `invalid success response is a sanitized permanent failure before persistence`(kind: String) {
        whenever(adapter.arkistoi(submission)).thenReturn(ArkistointiResult.Success(ArkistointiExternalIdTestData.value(kind)))
        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }
        assertThat(exception.retryable).isFalse()
        assertThat(exception.error.code).isEqualTo("ARCHIVE_ADAPTER_RESPONSE_INVALID")
        assertThat(exception.error.description).doesNotContain("private-response")
        assertThat(exception.cause).isNull()
        verify(state, never()).succeed(any(), anyOrNull())
        verify(state, never()).fail(any(), any(), any())
    }

    @Test
    fun `permanent adapter result is handed to the failure handler without a processor state write`() {
        val error = ArkistointiError.create("REJECTED", "Aineisto hylattiin")
        whenever(adapter.arkistoi(submission)).thenReturn(ArkistointiResult.PermanentFailure(error))
        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }
        assertThat(exception.error).isEqualTo(error)
        assertThat(exception.retryable).isFalse()
        verify(state, never()).fail(any(), any(), any())
    }

    @ParameterizedTest
    @EnumSource(ArkistointiAsiakirjaErrorCategory::class)
    fun `document category determines retries without retaining personal details`(category: ArkistointiAsiakirjaErrorCategory) {
        val original = ArkistointiAsiakirjaException(ArkistointiError.create("ARCHIVE_DOCUMENT_MISSING", "private-filename.pdf"), category)
        original.addSuppressed(IllegalStateException("private-filename.pdf"))
        whenever(state.prepare(execution)).thenThrow(original)
        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }
        assertThat(exception.retryable).isEqualTo(category == ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE)
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
        assertThat(exception.error.description).doesNotContain("private-filename")
        assertThat(exception.cause).isNull()
        assertThat(exception.suppressed).isEmpty()
        assertThat(exception.stackTraceToString()).doesNotContain("private-filename")
        verify(adapter, never()).arkistoi(any())
        verify(state, never()).fail(any(), any(), any())
    }

    @Test
    fun `success commit failure stays retryable without exposing the database exception`() {
        whenever(adapter.arkistoi(submission)).thenReturn(ArkistointiResult.Success("valid-id"))
        doThrow(IllegalStateException("private database detail")).`when`(state).succeed(execution, "valid-id")
        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }
        assertThat(exception.retryable).isTrue()
        assertThat(exception.error.code).isEqualTo("ARCHIVE_PROCESSING_FAILED")
        assertThat(exception.cause).isNull()
        assertThat(exception.message).doesNotContain("private")
    }
}
