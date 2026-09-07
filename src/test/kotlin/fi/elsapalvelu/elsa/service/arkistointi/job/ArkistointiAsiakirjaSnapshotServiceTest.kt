package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
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

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}
