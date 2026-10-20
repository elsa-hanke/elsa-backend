package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfCachingResourceRetriever
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfServiceImpl
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfEncryptedAttachmentTest {

    private val service = PdfServiceImpl(
        mock(SpringTemplateEngine::class.java),
        PdfGenerationMetricsService(SimpleMeterRegistry()),
        PdfContentValidator(),
        mock(PdfTextFieldValidator::class.java),
        mock(PdfCachingResourceRetriever::class.java)
    )

    @Test
    fun `merges a permission-restricted attachment and preserves the stored original`() {
        val original = PdfTestData.certificate(openingPassword = "")
        val attachment = attachment(original)
        val output = ByteArrayOutputStream()

        service.yhdistaAsiakirjat(listOf(attachment), output)

        assertPages(output.toByteArray(), PdfTestData.CERTIFICATE_TEXT)
        assertThat(attachment.asiakirjaData?.data).isEqualTo(original)
        Loader.loadPDF(original).use { assertThat(it.isEncrypted).isTrue }
    }

    @Test
    fun `appends a permission-restricted PDF to the generated document`() {
        val source = PdfTestData.certificate(text = "Generated summary")
        val certificate = PdfTestData.certificate(openingPassword = "")
        val output = ByteArrayOutputStream()

        service.yhdistaPdf(ByteArrayInputStream(source), ByteArrayInputStream(certificate), output)

        assertPages(output.toByteArray(), "Generated summary", PdfTestData.CERTIFICATE_TEXT)
    }

    @Test
    fun `rejects a password-required attachment`() {
        val attachment = attachment(PdfTestData.certificate(openingPassword = "required-password"))

        assertThatThrownBy { service.yhdistaAsiakirjat(listOf(attachment), ByteArrayOutputStream()) }
            .isInstanceOf(InvalidPdfAttachmentException::class.java)
    }

    @Test
    fun `does not append a PDF requiring an opening password`() {
        val source = PdfTestData.certificate()
        val protectedPdf = PdfTestData.certificate(openingPassword = "required-password")

        assertThatThrownBy {
            service.yhdistaPdf(
                ByteArrayInputStream(source),
                ByteArrayInputStream(protectedPdf),
                ByteArrayOutputStream()
            )
        }.isInstanceOf(InvalidPasswordException::class.java)
    }

    private fun attachment(data: ByteArray) = Asiakirja(
        id = 123L,
        nimi = "certificate.pdf",
        tyyppi = MediaType.APPLICATION_PDF_VALUE,
        asiakirjaData = AsiakirjaData(data = data)
    )

    private fun assertPages(data: ByteArray, vararg expectedText: String) {
        Loader.loadPDF(data).use { document ->
            assertThat(document.isEncrypted).isFalse
            assertThat(document.numberOfPages).isEqualTo(expectedText.size)
            expectedText.forEachIndexed { index, text ->
                val extractor = PDFTextStripper().apply {
                    startPage = index + 1
                    endPage = index + 1
                }
                assertThat(extractor.getText(document).trim()).isEqualTo(text)
            }
        }
    }
}
