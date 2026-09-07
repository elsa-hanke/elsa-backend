package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfServiceImplPdfATest {

    private val pdfA2bService = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))
    private val service = PdfServiceImpl(
        templateEngine = mock<SpringTemplateEngine>(),
        pdfMetrics = PdfGenerationMetricsService(SimpleMeterRegistry()),
        pdfContentValidator = PdfContentValidator(),
        pdfTextFieldValidator = mock<PdfTextFieldValidator>(),
        pdfA2bService = pdfA2bService
    )

    @Test
    fun `merged attachments are normalized to PDF-A 2b`() {
        val output = ByteArrayOutputStream()
        val asiakirjat = listOf(
            Asiakirja(
                id = 1L,
                nimi = "attachment.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                asiakirjaData = AsiakirjaData(data = fixture("valid.pdf"))
            ),
            Asiakirja(
                id = 2L,
                nimi = "attachment.jpg",
                tyyppi = MediaType.IMAGE_JPEG_VALUE,
                asiakirjaData = AsiakirjaData(data = fixture("valid.jpg"))
            )
        )

        service.yhdistaAsiakirjat(asiakirjat, output)

        assertThat(pdfA2bService.isPdfA2b(output.toByteArray())).isTrue
    }

    @Test
    fun `appended generated sections remain PDF-A 2b`() {
        val output = ByteArrayOutputStream()

        service.yhdistaPdf(
            ByteArrayInputStream(fixture("valid.pdf")),
            ByteArrayInputStream(fixture("valid.pdf")),
            output
        )

        assertThat(pdfA2bService.isPdfA2b(output.toByteArray())).isTrue
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}
