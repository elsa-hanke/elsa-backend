package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.io.IOException

class ArkistointiAsiakirjaErrorClassificationTest {
    private val repository = mock<AsiakirjaRepository>()
    private val jdbc = mock<JdbcTemplate>()
    private val pdfA = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))
    private val service = ArkistointiAsiakirjaSnapshotService(repository, pdfA)
    private val privateValue = "private-patient-name-secret.pdf"

    @ParameterizedTest
    @ValueSource(strings = ["missing", "empty", "unsupported", "corrupt-pdf", "corrupt-image", "encrypted"])
    fun `rejected source has a stable safe input error without a library cause`(kind: String) {
        val data = when (kind) {
            "missing" -> null
            "empty" -> byteArrayOf()
            "encrypted" -> PdfTestData.certificate(openingPassword = "secret")
            else -> privateValue.toByteArray()
        }
        val type = when (kind) {
            "unsupported" -> "application/$privateValue"
            "corrupt-image" -> "image/jpeg"
            else -> "application/pdf"
        }
        val exception = assertThrows<ArkistointiAsiakirjaException> { snapshot(service, data, type) }

        val code = when (kind) {
            "missing" -> "ARCHIVE_DOCUMENT_MISSING"
            "empty" -> "ARCHIVE_DOCUMENT_EMPTY"
            else -> "ARCHIVE_DOCUMENT_INVALID"
        }
        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT, code)
        verifyNoInteractions(repository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["pdf", "image"])
    fun `unavailable server color profile is not an input document error`(kind: String) {
        val profile = mock<Resource>()
        whenever(profile.inputStream).thenThrow(IOException(privateValue))
        val brokenService = ArkistointiAsiakirjaSnapshotService(repository, PdfA2bService(profile))
        val data = if (kind == "pdf") PdfTestData.unembeddedFontPdf(1) else ArchiveImageTestData.image()
        val type = if (kind == "pdf") "application/pdf" else "image/jpeg"

        val exception = assertThrows<ArkistointiAsiakirjaException> { snapshot(brokenService, data, type) }

        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE, "ARCHIVE_DOCUMENT_PROCESSING_FAILED")
        verifyNoInteractions(repository)
    }

    @Test
    fun `unexpected normalizer failure is sanitized but not classified as user input`() {
        val normalizer = mock<PdfA2bService>()
        whenever(normalizer.normalize(any(), any())).thenThrow(IllegalStateException(privateValue, IOException(privateValue)))
        val brokenService = ArkistointiAsiakirjaSnapshotService(repository, normalizer)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            snapshot(brokenService, byteArrayOf(1), "application/pdf")
        }

        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE, "ARCHIVE_DOCUMENT_PROCESSING_FAILED")
    }

    @Test
    fun `corrupt persisted snapshot is not reclassified as bad input`() {
        val stored = Asiakirja(id = 1L, nimi = privateValue, tyyppi = "application/pdf", archiveSnapshot = true,
            asiakirjaData = AsiakirjaData(data = PdfTestData.unembeddedFontPdf(1)))
        whenever(repository.findOneWithDataById(1L)).thenReturn(stored)
        whenever(repository.findArchiveSnapshotFlagById(1L)).thenReturn(true)

        val exception = assertThrows<ArkistointiAsiakirjaException> { service.createReference(stored, RecordType.LIITE, 0) }

        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT, "ARCHIVE_DOCUMENT_NOT_PDFA_2B")
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "empty", "checksum"])
    fun `loader failures are stored document errors even when their codes also describe input`(kind: String) {
        val stored = Asiakirja(id = 1L, nimi = privateValue,
            asiakirjaData = AsiakirjaData(data = if (kind == "empty") byteArrayOf() else byteArrayOf(1)))
        whenever(jdbc.query(any<String>(), any<RowMapper<ByteArray?>>(), eq(1L)))
            .thenReturn(if (kind == "missing") emptyList() else listOf(stored.asiakirjaData!!.data))
        val reference = ArkistointiJobAsiakirja(asiakirja = stored, filename = privateValue, sha256 = "0".repeat(64))

        val exception = assertThrows<ArkistointiAsiakirjaException> { ArkistointiAsiakirjaLoaderImpl(jdbc).load(reference) }

        val code = when (kind) {
            "missing" -> "ARCHIVE_DOCUMENT_MISSING"
            "empty" -> "ARCHIVE_DOCUMENT_EMPTY"
            else -> "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH"
        }
        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT, code)
    }

    @Test
    fun `repository outage is not converted into an input error`() {
        val failure = DataAccessResourceFailureException("database unavailable")
        whenever(jdbc.query(any<String>(), any<RowMapper<ByteArray?>>(), eq(1L))).thenThrow(failure)
        val exception = assertThrows<DataAccessResourceFailureException> {
            ArkistointiAsiakirjaLoaderImpl(jdbc).load(ArkistointiJobAsiakirja(asiakirja = Asiakirja(id = 1L)))
        }
        assertThat(exception).isSameAs(failure)
    }

    private fun snapshot(target: ArkistointiAsiakirjaSnapshotService, data: ByteArray?, type: String) =
        target.createSnapshot(Opintooikeus(id = 1L), privateValue, type, data, RecordType.LIITE, 0)

    private fun assertSafe(exception: ArkistointiAsiakirjaException, category: ArkistointiAsiakirjaErrorCategory, code: String) {
        assertThat(exception.category).isEqualTo(category)
        assertThat(exception.errorKey).isEqualTo(code)
        assertThat(exception.message).isEqualTo(code)
        assertThat(exception.error.description).doesNotContain(privateValue)
        assertThat(exception.cause).isNull()
        assertThat(exception.suppressed).isEmpty()
        assertThat(exception.stackTraceToString()).doesNotContain(privateValue)
    }
}
