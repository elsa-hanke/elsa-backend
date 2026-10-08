package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.ThymeleafConfiguration
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
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith

class PdfRealTemplateGenerationTest {
    private val engine = ThymeleafConfiguration().templateEngine().apply {
        setTemplateEngineMessageSource(ResourceBundleMessageSource().apply {
            setBasename("i18n/messages")
            setDefaultEncoding("UTF-8")
            setFallbackToSystemLocale(false)
        })
    }
    private val validator = PdfTextTestSupport.fieldValidator()
    private val service = PdfServiceImpl(
        engine, PdfGenerationMetricsService(SimpleMeterRegistry()), PdfContentValidator(), validator,
        PdfCachingResourceRetriever()
    ).apply {
        colorProfile = ClassPathResource("sRGB_CS_profile.icm")
        liberationSerifFont = ClassPathResource("fonts/LiberationSerif-Regular.ttf")
        liberationSerifFontBold = ClassPathResource("fonts/LiberationSerif-Bold.ttf")
        notoSansFont = ClassPathResource("fonts/NotoSans-Regular.ttf")
        notoSansFontItalic = ClassPathResource("fonts/NotoSans-Italic.ttf")
    }

    @ParameterizedTest
    @MethodSource("templates")
    fun `real Finnish templates preserve accepted text through HTML rewriting and PDF generation`(template: String) {
        val text = "Ääkköset – Jyväskylä → • €"
        validator.validate(listOf("kentta" to text))
        val output = ByteArrayOutputStream()
        service.luoPdf(template, PdfTemplateTestFixture(text).context(template), output)

        Loader.loadPDF(output.toByteArray()).use { document ->
            assertThat(document.numberOfPages).isGreaterThan(0)
            val extracted = PDFTextStripper().getText(document).replace(Regex("\\s+"), " ")
            assertThat(extracted).contains(text)
            assertThat(extracted).doesNotContain("�", "??")
            val xmp = document.documentCatalog.metadata.exportXMPMetadata().use { it.readAllBytes() }
            assertThat(String(xmp, StandardCharsets.UTF_8)).contains("pdfaid:part", "pdfaid:conformance")
        }
    }

    @ParameterizedTest
    @MethodSource("templates")
    fun `real templates reject legacy unsupported text before writing PDF bytes`(template: String) {
        val output = ByteArrayOutputStream()
        val error = assertFailsWith<UnsupportedPdfCharactersException> {
            service.luoPdf(template, PdfTemplateTestFixture("💗").context(template), output)
        }
        assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(output.size()).isZero()
    }

    @Test
    fun `template matrix covers every PDF template resource`() {
        // New templates must be added to this matrix instead of silently losing coverage.
        val resources = org.springframework.core.io.support.PathMatchingResourcePatternResolver()
            .getResources("classpath*:templates/pdf/**/*.html")
            .map { it.url.toString().substringAfter("templates/") }.toSet()
        assertThat(templates().toSet()).isEqualTo(resources)
    }

    companion object {
        @JvmStatic
        fun templates(): List<String> = listOf(
            "pdf/koulutussopimus.html", "pdf/vastuuhenkilonarvio.html",
            "pdf/valmistumisenyhteenveto.html", "pdf/valmistumisenyhteenveto_yek.html",
            "pdf/erikoistujantiedot/koulutussuunnitelma.html", "pdf/erikoistujantiedot/paivittaisetmerkinnat.html",
            "pdf/erikoistujantiedot/arviointi.html", "pdf/erikoistujantiedot/arvioinnit.html",
            "pdf/erikoistujantiedot/seurantajakso.html", "pdf/erikoistujantiedot/suoritemerkinta.html",
            "pdf/erikoistujantiedot/suoritemerkinnat.html"
        )
    }
}
