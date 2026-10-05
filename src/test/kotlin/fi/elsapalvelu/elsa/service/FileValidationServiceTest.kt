package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.impl.kayttaja.FileValidationServiceImpl
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile

class FileValidationServiceTest {
    private val asiakirjaService = mock(AsiakirjaService::class.java)
    private val service = FileValidationServiceImpl(asiakirjaService, PdfContentValidator())

    @BeforeEach
    fun setup() {
        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `accepts a valid PDF`(withOpintooikeus: Boolean) {
        validate(pdf(PdfTestData.certificate()), withOpintooikeus)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `accepts copying restrictions when no opening password is required`(withOpintooikeus: Boolean) {
        validate(pdf(PdfTestData.certificate(openingPassword = "")), withOpintooikeus)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `identifies an opening password separately from invalid PDF data`(withOpintooikeus: Boolean) {
        assertError("pdf-tiedosto-vaatii-salasanan", withOpintooikeus) {
            pdf(PdfTestData.certificate(openingPassword = "test-password"))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `identifies non-PDF bytes labelled as PDF`(withOpintooikeus: Boolean) {
        assertError("pdf-tiedostoa-ei-voitu-kasitella", withOpintooikeus) { pdf("not a PDF".toByteArray()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `identifies truncated PDF data`(withOpintooikeus: Boolean) {
        assertError("pdf-tiedostoa-ei-voitu-kasitella", withOpintooikeus) {
            pdf(PdfTestData.certificate().copyOf(20))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `identifies empty files before PDF processing`(withOpintooikeus: Boolean) {
        assertError("tiedosto-on-tyhja", withOpintooikeus) { pdf(ByteArray(0)) }
        assertError("tiedosto-on-tyhja", withOpintooikeus) {
            MockMultipartFile("file", "empty.png", MediaType.IMAGE_PNG_VALUE, ByteArray(0))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `identifies an unsupported or missing content type`(withOpintooikeus: Boolean) {
        listOf("text/plain", null).forEach { contentType ->
            assertError("tiedostotyyppi-ei-ole-sallittu", withOpintooikeus) {
                MockMultipartFile("file", "unsupported.txt", contentType, "content".toByteArray())
            }
        }
    }

    @Test
    fun `identifies a duplicate name within the study right`() {
        `when`(asiakirjaService.findAllByOpintooikeusId(1L))
            .thenReturn(listOf(AsiakirjaDTO(nimi = "certificate.pdf")))
        assertError("samanniminen-tiedosto-on-jo-olemassa", true) { pdf(PdfTestData.certificate()) }
    }

    @Test
    fun `accepts several valid files and supported image types`() {
        val images = listOf("image/png", "image/jpeg", "image/jpg").mapIndexed { index, type ->
            MockMultipartFile("file", "image-$index", type, "content".toByteArray())
        }
        service.validate(images + pdf(PdfTestData.certificate()), 1L)
        service.validate(images + pdf(PdfTestData.certificate()))
    }

    @Test
    fun `honours custom allowed types`() {
        val xml = MockMultipartFile("file", "data.xml", "application/xml", "<data/>".toByteArray())
        service.validate(listOf(xml), listOf("application/xml"))
        service.validate(listOf(xml), 1L, listOf("application/xml"))
        val exception = assertThrows<BadRequestAlertException> {
            service.validate(listOf(xml), listOf(MediaType.APPLICATION_PDF_VALUE))
        }
        assertThat(exception.errorKey).isEqualTo("dataillegal.tiedostotyyppi-ei-ole-sallittu")
    }

    @Test
    fun `retains the existing filename length check`() {
        val name = "a".repeat(256) + ".pdf"
        val file = MockMultipartFile(name, name, MediaType.APPLICATION_PDF_VALUE, PdfTestData.certificate())
        assertError("tiedosto-ei-ole-kelvollinen", true) { file }
        assertError("tiedosto-ei-ole-kelvollinen", false) { file }
    }

    private fun pdf(data: ByteArray) =
        MockMultipartFile("file", "certificate.pdf", MediaType.APPLICATION_PDF_VALUE, data)

    private fun validate(file: MockMultipartFile, withOpintooikeus: Boolean) {
        if (withOpintooikeus) service.validate(listOf(file), 1L) else service.validate(listOf(file))
    }

    private fun assertError(key: String, withOpintooikeus: Boolean, file: () -> MockMultipartFile) {
        val exception = assertThrows<BadRequestAlertException> { validate(file(), withOpintooikeus) }
        assertThat(exception.errorKey).isEqualTo("dataillegal.$key")
    }
}
