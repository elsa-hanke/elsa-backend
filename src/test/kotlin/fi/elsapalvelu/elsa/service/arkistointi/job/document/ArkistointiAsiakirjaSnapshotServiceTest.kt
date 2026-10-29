package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

class ArkistointiAsiakirjaSnapshotServiceTest {
    private val repository = mock<AsiakirjaRepository>()
    private val service = ArkistointiAsiakirjaSnapshotService(repository)

    @ParameterizedTest
    @ValueSource(strings = ["pdf", "restricted-pdf", "jpeg", "png", "alias", "large-jpeg", "cmyk", "sixteen-bit", "oriented"])
    fun `snapshot preserves exact bytes filename and content type`(kind: String) {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { it.getArgument<Asiakirja>(0).apply { id = 123L } }
        val (type, original) = when (kind) {
            "pdf" -> "application/pdf" to PdfTestData.unembeddedFontPdf(100)
            "restricted-pdf" -> "application/pdf" to PdfTestData.certificate(openingPassword = "")
            "png" -> "image/png" to ArchiveImageTestData.image("png")
            "alias" -> "image/jpg" to ArchiveImageTestData.image()
            "large-jpeg" -> "image/jpeg" to ArchiveImageTestData.image("jpeg", 4000, 3000)
            "cmyk" -> "image/jpeg" to PdfTestData.cmykJpeg()
            "sixteen-bit" -> "image/png" to ArchiveImageTestData.sixteenBitPng()
            "oriented" -> "image/jpeg" to ArchiveImageTestData.orientedJpeg(6)
            else -> "image/jpeg" to ArchiveImageTestData.image()
        }
        val filename = "document." + type.substringAfter('/')
        val unchanged = original.copyOf()

        val reference = service.createSnapshot(
            Opintooikeus(id = 10L), filename, type, original, RecordType.LIITE, 2
        )

        assertThat(reference.asiakirjatyyppi).isEqualTo(RecordType.LIITE)
        assertThat(reference.jarjestysnumero).isEqualTo(2)
        assertThat(reference.filename).isEqualTo(filename)
        assertThat(reference.contentType).isEqualTo(type)
        val bytes = reference.asiakirja.asiakirjaData!!.data!!
        assertThat(bytes).containsExactly(*unchanged).isNotSameAs(original)
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(unchanged))
        assertThat(reference.asiakirja.archiveSnapshot).isTrue
        original[0] = (original[0].toInt() xor 1).toByte()
        assertThat(bytes).containsExactly(*unchanged)
    }

    @Test
    fun `missing snapshot data fails with a controlled input error`() {
        val exception = assertThrows<ArkistointiAsiakirjaException> {
            service.createSnapshot(Opintooikeus(id = 10L), "document.pdf", "application/pdf", null, RecordType.LIITE, 0)
        }
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
        assertThat(exception.category).isEqualTo(ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT)
        verifyNoInteractions(repository)
    }

    @ParameterizedTest
    @ValueSource(ints = [30, 100, 201])
    fun `ordinary PDF is copied into a distinct immutable snapshot without conversion`(pages: Int) {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { it.getArgument<Asiakirja>(0).apply { id = 125L } }
        val original = PdfTestData.unembeddedFontPdf(pages)
        val source = Asiakirja(
            id = 4L, nimi = "attachment.pdf", tyyppi = "application/pdf",
            opintooikeus = Opintooikeus(id = 10L), asiakirjaData = AsiakirjaData(data = original)
        )
        whenever(repository.findOneWithDataById(4L)).thenReturn(source)

        val reference = service.createReference(source, RecordType.LIITE, 0)

        assertThat(reference.asiakirja.id).isEqualTo(125L)
        assertThat(reference.asiakirja.archiveSnapshot).isTrue
        assertThat(reference.asiakirja.asiakirjaData!!.data).containsExactly(*original).isNotSameAs(original)
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(original))
        assertThat(source.archiveSnapshot).isFalse
    }

    @Test
    fun `existing immutable ordinary PDF is reused without requiring PDF-A`() {
        val snapshot = Asiakirja(
            id = 4L, nimi = "snapshot.pdf", tyyppi = "application/pdf",
            asiakirjaData = AsiakirjaData(data = PdfTestData.unembeddedFontPdf(100)), archiveSnapshot = true
        )
        whenever(repository.findOneWithDataById(4L)).thenReturn(snapshot)
        whenever(repository.findArchiveSnapshotFlagById(4L)).thenReturn(true)

        val reference = service.createReference(snapshot, RecordType.LIITE, 0)

        assertThat(reference.asiakirja).isSameAs(snapshot)
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(snapshot.asiakirjaData!!.data!!))
        verify(repository, never()).save(any<Asiakirja>())
    }

    @Test
    fun `resolved snapshot is hashed once and loader independently verifies its bytes`() {
        val data = PdfTestData.unembeddedFontPdf(1)
        val snapshot = Asiakirja(id = 4L, nimi = "snapshot.pdf", tyyppi = "application/pdf",
            asiakirjaData = AsiakirjaData(data = data), archiveSnapshot = true)
        whenever(repository.findOneWithDataById(4L)).thenReturn(snapshot)
        whenever(repository.findArchiveSnapshotFlagById(4L)).thenReturn(true)
        val original = service.createReference(snapshot, RecordType.LIITE, 0)
        val jdbc = mock<JdbcTemplate>()
        whenever(jdbc.query(any<String>(), any<RowMapper<ByteArray?>>(), eq(4L))).thenReturn(listOf(data))

        Mockito.mockStatic(DigestUtils::class.java, Mockito.CALLS_REAL_METHODS).use { digests ->
            val resolved = service.resolveReference(original.copy(sha256 = original.sha256.uppercase()))
            assertThat(resolved.sha256).isEqualTo(original.sha256)
            digests.verify({ DigestUtils.sha256Hex(data) }, times(1))

            val reference = ArkistointiDocumentDescriptor(
                resolved.asiakirja.id!!, resolved.asiakirjatyyppi, resolved.jarjestysnumero,
                resolved.filename, resolved.contentType, resolved.sha256
            )
            val loader = ArkistointiAsiakirjaLoaderImpl(jdbc)
            assertThat(loader.load(reference).sisalto).containsExactly(*data)
            digests.verify({ DigestUtils.sha256Hex(data) }, times(2))

            data[0] = (data[0].toInt() xor 1).toByte()
            val exception = assertThrows<ArkistointiAsiakirjaException> { loader.load(reference) }
            assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
            digests.verify({ DigestUtils.sha256Hex(data) }, times(3))
        }
    }
}
