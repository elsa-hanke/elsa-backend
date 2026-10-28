package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaLoader
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentDescriptor
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArkistointiJobProcessorImplTest {
    private val state = mock<ArkistointiJobStateService>()
    private val loader = mock<ArkistointiAsiakirjaLoader>()
    private val adapter = mock<ArkistointiAdapter>().apply {
        whenever(university).thenReturn(YliopistoEnum.TURUN_YLIOPISTO)
        whenever(supportedCaseTypes).thenReturn(setOf(CaseType.VALMISTUMINEN))
    }
    private val processor = ArkistointiJobProcessorImpl(state, loader, ArkistointiAdapterRegistry(listOf(adapter)))
    private val jobId = UUID.randomUUID()
    private val execution = ArkistointiJobExecution("arkistointi-job", jobId.toString(), 1L)
    private val descriptor = ArkistointiDocumentDescriptor(1L, RecordType.LIITE, 0, "private.pdf", "application/pdf", "a".repeat(64))
    private val document = LoadedArkistointiAsiakirja(1L, RecordType.LIITE, 0, descriptor.filename,
        descriptor.contentType, descriptor.sha256, byteArrayOf(1))
    private val submission = ArkistointiSubmission(jobId, YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", listOf(document))

    @BeforeEach
    fun prepareSubmission() {
        whenever(state.prepare(execution)).thenReturn(
            PreparedArkistointiJob(jobId, submission.university, submission.caseType, submission.payload, listOf(descriptor))
        )
        whenever(loader.load(descriptor)).thenReturn(document)
        whenever(state.checkBeforeSend(execution)).thenReturn(true)
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
    fun `unsupported case type is a permanent failure before loading documents or sending`() {
        whenever(adapter.supportedCaseTypes).thenReturn(setOf(CaseType.SOPIMUS))

        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }

        assertThat(exception.retryable).isFalse()
        assertThat(exception.error.code).isEqualTo("ARCHIVE_CASE_TYPE_UNSUPPORTED")
        verify(loader, never()).load(any())
        verify(adapter, never()).arkistoi(any())
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
        whenever(loader.load(descriptor)).thenThrow(original)
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
    fun `execution replaced during loading is rejected before delivery`() {
        whenever(state.checkBeforeSend(execution)).thenThrow(StaleArkistointiExecutionException())
        assertThrows<StaleArkistointiExecutionException> { processor.process(execution) }
        verify(loader).load(descriptor)
        verify(adapter, never()).arkistoi(any())
        verify(state, never()).succeed(any(), anyOrNull())
        verify(state, never()).fail(any(), any(), any())
    }

    @Test
    fun `terminal job detected after loading is not delivered`() {
        whenever(state.checkBeforeSend(execution)).thenReturn(false)
        processor.process(execution)
        verify(loader).load(descriptor)
        verify(adapter, never()).arkistoi(any())
        verify(state, never()).succeed(any(), anyOrNull())
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

    @ParameterizedTest
    @ValueSource(strings = ["io", "state", "argument"])
    fun `unknown adapter exception stays retryable and loses unsafe diagnostic details`(kind: String) {
        doAnswer {
            throw when (kind) {
                "io" -> java.io.IOException("private disk details")
                "state" -> IllegalStateException("private state details")
                else -> IllegalArgumentException("private library details")
            }
        }.whenever(adapter).arkistoi(submission)

        val exception = assertThrows<ArkistointiProcessingException> { processor.process(execution) }

        assertThat(exception.retryable).isTrue()
        assertThat(exception.error.code).isEqualTo("ARCHIVE_PROCESSING_FAILED")
        assertThat(exception.cause).isNull()
        assertThat(exception.stackTraceToString()).doesNotContain("private")
        verify(state, never()).succeed(any(), anyOrNull())
    }
}
