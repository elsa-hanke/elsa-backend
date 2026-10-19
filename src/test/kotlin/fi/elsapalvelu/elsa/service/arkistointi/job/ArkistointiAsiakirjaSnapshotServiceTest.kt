package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType

@ExtendWith(MockitoExtension::class)
class ArkistointiAsiakirjaSnapshotServiceTest {

    @Mock
    private lateinit var repository: AsiakirjaRepository

    private lateinit var service: ArkistointiAsiakirjaSnapshotService

    @BeforeEach
    fun setUp() {
        service = ArkistointiAsiakirjaSnapshotService(
            repository,
            PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))
        )
    }

    @Test
    fun `creates an immutable PDF-A snapshot of the licensing certificate`() {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { invocation ->
            invocation.getArgument<Asiakirja>(0).apply { id = 123L }
        }
        val certificate = fixture("valid.jpg")
        val erikoistuvaLaakari = ErikoistuvaLaakari(
            laillistamistodistus = AsiakirjaData(data = certificate),
            laillistamispaivanLiitetiedostonNimi = "licence.jpg",
            laillistamispaivanLiitetiedostonTyyppi = MediaType.IMAGE_JPEG_VALUE
        )
        val opintooikeus = Opintooikeus(id = 10L, erikoistuvaLaakari = erikoistuvaLaakari)

        val reference = service.createLaillistamistodistusReference(opintooikeus, 2)

        assertThat(reference.asiakirjatyyppi).isEqualTo(RecordType.LAILLISTAMISTODISTUS)
        assertThat(reference.jarjestysnumero).isEqualTo(2)
        assertThat(reference.filename).isEqualTo("licence.pdf")
        assertThat(reference.contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(reference.sha256).isEqualTo(
            ArkistointiChecksum.sha256(reference.asiakirja.asiakirjaData?.data!!)
        )
        assertThat(reference.asiakirja.asiakirjaData?.data).isNotEqualTo(certificate)
        assertThat(reference.asiakirja.archiveSnapshot).isTrue
    }

    @Test
    fun `snapshot supports a certificate with copying restrictions`() {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { invocation ->
            invocation.getArgument<Asiakirja>(0).apply { id = 124L }
        }
        val original = PdfTestData.certificate(openingPassword = "")
        val reference = service.createLaillistamistodistusReference(
            Opintooikeus(
                id = 10L,
                erikoistuvaLaakari = ErikoistuvaLaakari(
                    laillistamistodistus = AsiakirjaData(data = original),
                    laillistamispaivanLiitetiedostonNimi = "certificate.pdf",
                    laillistamispaivanLiitetiedostonTyyppi = MediaType.APPLICATION_PDF_VALUE
                )
            ), 0
        )
        assertThat(reference.asiakirja.archiveSnapshot).isTrue
        assertThat(PdfA2bService(ClassPathResource("sRGB_CS_profile.icm")).isPdfA2b(reference.asiakirja.asiakirjaData!!.data)).isTrue
        org.apache.pdfbox.Loader.loadPDF(original).use { assertThat(it.isEncrypted).isTrue }
    }

    @Test
    fun `missing licensing certificate fails with a controlled error`() {
        val opintooikeus = Opintooikeus(
            erikoistuvaLaakari = ErikoistuvaLaakari(laillistamistodistus = null)
        )

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            service.createLaillistamistodistusReference(opintooikeus, 0)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
        verifyNoInteractions(repository)
    }

    @Test
    fun `reference creation rejects an empty document`() {
        val asiakirja = Asiakirja(
            id = 4L,
            nimi = "empty.pdf",
            tyyppi = MediaType.APPLICATION_PDF_VALUE,
            asiakirjaData = AsiakirjaData(data = byteArrayOf())
        )

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            service.createReference(asiakirja, RecordType.LIITE, 0)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_EMPTY")
    }

    @Test
    fun `ordinary attachment is normalized only in a separate immutable snapshot`() {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { invocation ->
            invocation.getArgument<Asiakirja>(0).apply { id = 125L }
        }
        val original = PdfTestData.unembeddedFontPdf(1)
        val source = Asiakirja(
            id = 4L, nimi = "attachment.pdf", tyyppi = MediaType.APPLICATION_PDF_VALUE,
            opintooikeus = Opintooikeus(id = 10L), asiakirjaData = AsiakirjaData(data = original)
        )

        val reference = service.createReference(source, RecordType.LIITE, 0)

        val snapshot = reference.asiakirja
        val snapshotData = snapshot.asiakirjaData!!.data!!
        assertThat(snapshot.id).isEqualTo(125L)
        assertThat(snapshot.archiveSnapshot).isTrue
        assertThat(snapshot.tyyppi).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(PdfA2bService(ClassPathResource("sRGB_CS_profile.icm")).isPdfA2b(snapshotData)).isTrue
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(snapshotData))
        assertThat(source.asiakirjaData!!.data).containsExactly(*original)
        assertThat(source.archiveSnapshot).isFalse
    }

    @Test
    fun `already conforming PDF is copied into a snapshot without changing bytes`() {
        whenever(repository.save(any<Asiakirja>())).thenAnswer { invocation ->
            invocation.getArgument<Asiakirja>(0).apply { id = 126L }
        }
        val data = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))
            .normalize(PdfTestData.certificate(), MediaType.APPLICATION_PDF_VALUE)
        val source = Asiakirja(
            id = 4L, nimi = "attachment.pdf", tyyppi = MediaType.APPLICATION_PDF_VALUE,
            opintooikeus = Opintooikeus(id = 10L), asiakirjaData = AsiakirjaData(data = data)
        )

        val reference = service.createReference(source, RecordType.LIITE, 0)

        assertThat(reference.asiakirja).isNotSameAs(source)
        assertThat(reference.asiakirja.asiakirjaData!!.data).containsExactly(*data)
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(data))
    }

    @Test
    fun `nonconforming immutable snapshot is rejected rather than repaired`() {
        val snapshot = Asiakirja(
            id = 4L, nimi = "snapshot.pdf", tyyppi = MediaType.APPLICATION_PDF_VALUE,
            asiakirjaData = AsiakirjaData(data = PdfTestData.unembeddedFontPdf(1)), archiveSnapshot = true
        )

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            service.createReference(snapshot, RecordType.LIITE, 0)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_NOT_PDFA_2B")
        verifyNoInteractions(repository)
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}
