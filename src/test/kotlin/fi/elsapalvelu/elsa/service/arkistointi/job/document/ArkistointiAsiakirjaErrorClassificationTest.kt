package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
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
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

class ArkistointiAsiakirjaErrorClassificationTest {
    private val repository = mock<AsiakirjaRepository>()
    private val jdbc = mock<JdbcTemplate>()
    private val service = ArkistointiAsiakirjaSnapshotService(repository)
    private val privateValue = "private-patient-name-secret.pdf"

    @ParameterizedTest
    @ValueSource(strings = ["missing", "empty", "filename", "contentType", "long-filename", "long-contentType"])
    fun `incomplete snapshot has a stable safe input error`(kind: String) {
        val data = when (kind) {
            "missing" -> null
            "empty" -> byteArrayOf()
            else -> byteArrayOf(1)
        }
        val name = when (kind) {
            "filename" -> null
            "long-filename" -> "a".repeat(256)
            else -> privateValue
        }
        val type = when (kind) {
            "contentType" -> ""
            "long-contentType" -> "a".repeat(256)
            else -> "application/pdf"
        }
        val exception = assertThrows<ArkistointiAsiakirjaException> {
            service.createSnapshot(Opintooikeus(id = 1L), name, type, data, RecordType.LIITE, 0)
        }
        val code = when (kind) {
            "missing" -> "ARCHIVE_DOCUMENT_MISSING"
            "empty" -> "ARCHIVE_DOCUMENT_EMPTY"
            else -> "ARCHIVE_DOCUMENT_INVALID"
        }
        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT, code)
        verifyNoInteractions(repository)
    }

    @Test
    fun `empty persisted snapshot is not classified as bad input`() {
        val stored = Asiakirja(id = 1L, nimi = privateValue, tyyppi = "application/pdf", archiveSnapshot = true,
            asiakirjaData = AsiakirjaData(data = byteArrayOf()))
        val exception = assertThrows<ArkistointiAsiakirjaException> { service.createReference(stored, RecordType.LIITE, 0) }
        assertSafe(exception, ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT, "ARCHIVE_DOCUMENT_EMPTY")
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "empty", "checksum"])
    fun `loader failures are stored document errors even when their codes also describe input`(kind: String) {
        val stored = Asiakirja(id = 1L, nimi = privateValue,
            asiakirjaData = AsiakirjaData(data = if (kind == "empty") byteArrayOf() else byteArrayOf(1)))
        whenever(jdbc.query(any<String>(), any<RowMapper<ByteArray?>>(), eq(1L)))
            .thenReturn(if (kind == "missing") emptyList() else listOf(stored.asiakirjaData!!.data))
        val reference = ArkistointiDocumentDescriptor(1L, RecordType.LIITE, 0, privateValue, "application/pdf", "0".repeat(64))
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
            ArkistointiAsiakirjaLoaderImpl(jdbc).load(
                ArkistointiDocumentDescriptor(1L, RecordType.LIITE, 0, privateValue, "application/pdf", "0".repeat(64))
            )
        }
        assertThat(exception).isSameAs(failure)
    }

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
