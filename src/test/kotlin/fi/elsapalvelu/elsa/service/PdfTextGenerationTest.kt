package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfCachingResourceRetriever
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfServiceImpl
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.io.ClassPathResource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.ByteArrayOutputStream
import kotlin.test.assertFailsWith

class PdfTextGenerationTest {
    private val templateEngine = mock(SpringTemplateEngine::class.java)
    private val service = PdfServiceImpl(
        templateEngine,
        PdfGenerationMetricsService(SimpleMeterRegistry()),
        PdfContentValidator(),
        PdfTextTestSupport.fieldValidator(),
        PdfCachingResourceRetriever()
    ).apply {
        colorProfile = ClassPathResource("sRGB_CS_profile.icm")
        liberationSerifFont = ClassPathResource("fonts/LiberationSerif-Regular.ttf")
        liberationSerifFontBold = ClassPathResource("fonts/LiberationSerif-Bold.ttf")
        notoSansFont = ClassPathResource("fonts/NotoSans-Regular.ttf")
        notoSansFontItalic = ClassPathResource("fonts/NotoSans-Italic.ttf")
    }

    @ParameterizedTest
    @CsvSource(
        "pdf/koulutussopimus.html,pdf-osio-koejakson-koulutussopimus,koejaksonkoulutussopimus",
        "pdf/vastuuhenkilonarvio.html,pdf-osio-koejakson-vastuuhenkilon-arvio,koejaksonvastuuhenkilonarvio"
    )
    fun `existing unsupported koejakso text fails before writing PDF bytes`(template: String, field: String, source: String) {
        val context = Context()
        `when`(templateEngine.process(template, context)).thenReturn("<html><body><p>Vanha teksti 💗</p></body></html>")
        val output = ByteArrayOutputStream()

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.luoPdf(template, context, output) }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.pdfSource).isEqualTo(source)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(output.size()).isZero()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "pdf/koulutussopimus.html", "pdf/vastuuhenkilonarvio.html",
        "pdf/valmistumisenyhteenveto.html", "pdf/valmistumisenyhteenveto_yek.html",
        "pdf/erikoistujantiedot/arviointi.html"
    ])
    fun `encoded unsupported text cannot bypass PDF generation validation`(template: String) {
        val context = Context()
        `when`(templateEngine.process(template, context)).thenReturn("<p>Vanha teksti &#x1F497;</p>")
        val output = ByteArrayOutputStream()

        assertFailsWith<UnsupportedPdfCharactersException> { service.luoPdf(template, context, output) }
        assertThat(output.size()).isZero()
    }

    @ParameterizedTest
    @ValueSource(strings = ["pdf/koulutussopimus.html", "pdf/vastuuhenkilonarvio.html"])
    fun `valid Finnish formatted content still produces a readable PDF`(template: String) {
        val context = Context()
        `when`(templateEngine.process(template, context)).thenReturn(
            "<html><head><title>💗</title><style>/* 💗 */</style></head><body>" +
                "<p>Hyvä <strong>lääkäri</strong> – Jyväskylä &amp; kouluttaja</p></body></html>"
        )
        val output = ByteArrayOutputStream()
        service.luoPdf(template, context, output)

        Loader.loadPDF(output.toByteArray()).use { document ->
            assertThat(PDFTextStripper().getText(document)).contains("Hyvä lääkäri – Jyväskylä & kouluttaja")
        }
    }

    @Test
    fun `escaped entity text remains literal in generated PDF`() {
        val template = "pdf/vastuuhenkilonarvio.html"
        val context = Context()
        `when`(templateEngine.process(template, context)).thenReturn("<p>&amp;#x1F497;</p>")
        val output = ByteArrayOutputStream()
        service.luoPdf(template, context, output)

        Loader.loadPDF(output.toByteArray()).use { document ->
            assertThat(PDFTextStripper().getText(document)).contains("&#x1F497;")
        }
    }
}
