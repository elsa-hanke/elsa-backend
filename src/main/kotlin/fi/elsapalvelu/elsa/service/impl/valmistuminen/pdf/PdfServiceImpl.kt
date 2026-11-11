package fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf

import org.springframework.beans.factory.annotation.Value
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.io.exceptions.IOException
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.font.FontProvider
import com.itextpdf.layout.properties.ObjectFit
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.pdfa.PdfADocument
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfPreparation
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.valmistuminen.PdfAssembler
import fi.elsapalvelu.elsa.service.PdfHtmlText
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_LUO_PDF
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_ASIAKIRJAT
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_PDF
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentSource
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.*

@Service
class PdfServiceImpl(
    private val templateEngine: SpringTemplateEngine,
    private val pdfMetrics: PdfGenerationMetricsService,
    private val pdfContentValidator: PdfContentValidator,
    private val pdfTextFieldValidator: PdfTextFieldValidator,
    private val resourceRetriever: PdfCachingResourceRetriever
) : PdfService {

    private val log = LoggerFactory.getLogger(javaClass)


    /**
     * iTextin "smart mode": yhdistelyssa samanlaiset objektit (fonttien osajoukot,
     * varioprofiili, metatiedot) jaetaan sen sijaan etta ne kopioitaisiin jokaisesta
     * lahdedokumentista erikseen. Pienentaa erikoistujan tiedot -koosteen noin kolmasosaan.
     *
     * Kaytossa vain [ItextPdfAssembler]issa, jossa lahdedokumentit ovat sovelluksen itsensa
     * tuottamia ja rakenteeltaan yhdenmukaisia PDF/A-dokumentteja. Kayttajien lataamien
     * liitteiden yhdistelyssa tilaa ei kayteta: mitattu hyoty oli vain 1 MiB (3 -> 2 MiB),
     * ja liitteet ovat mielivaltaisia ulkopuolisia PDF-tiedostoja, joiden rakennetta ei
     * hallita.
     *
     * Katkaisin on olemassa siksi, etta tilan voi tarvittaessa kytkea pois ilman uutta
     * julkaisua.
     */
    @Value("\${elsa.pdf.smart-mode:true}")
    var smartMode: Boolean = true

    @Value("classpath:sRGB_CS_profile.icm")
    var colorProfile: Resource? = null

    @Value("classpath:fonts/LiberationSerif-Bold.ttf")
    var liberationSerifFontBold: Resource? = null

    @Value("classpath:fonts/LiberationSerif-Regular.ttf")
    var liberationSerifFont: Resource? = null

    @Value("classpath:fonts/NotoSans-Italic.ttf")
    var notoSansFontItalic: Resource? = null

    @Value("classpath:fonts/NotoSans-Regular.ttf")
    var notoSansFont: Resource? = null

    override fun luoPdf(template: String, context: Context, outputStream: OutputStream) {
        pdfMetrics.trackOperation(OP_LUO_PDF) {
            val content = sanitizeContent(templateEngine.process(template, context))
            if (isValidatedPdfTemplate(template)) {
                pdfTextFieldValidator.validate(
                    fields = emptyList(),
                    htmlFields = listOf(pdfSectionField(template) to content),
                    pdfSource = pdfSource(template)
                )
            }
            val pdf = PdfADocument(
                PdfWriter(outputStream),
                PdfAConformanceLevel.PDF_A_2B,
                PdfOutputIntent(
                    "Custom", "", "https://www.color.org",
                    "sRGB IEC61966-2.1", colorProfile?.inputStream
                )
            )
            val provider = FontProvider()
            provider.addFont(liberationSerifFont?.file?.absolutePath)
            provider.addFont(liberationSerifFontBold?.file?.absolutePath)
            provider.addFont(notoSansFont?.file?.absolutePath)
            provider.addFont(notoSansFontItalic?.file?.absolutePath)

            val properties = ConverterProperties()
            properties.fontProvider = provider
            properties.resourceRetriever = resourceRetriever

            HtmlConverter.convertToPdf(content, pdf, properties)
        }
    }

    override fun yhdistaAsiakirjat(
        asiakirjat: List<Asiakirja>,
        outputStream: OutputStream
    ) {
        pdfMetrics.trackOperation(OP_YHDISTA_ASIAKIRJAT) {
            // Tahan ei kayteta smart modea: lahteet ovat kayttajien lataamia, mielivaltaisia
            // PDF-tiedostoja, joissa on vain vahan jaettavaa (mitattu hyoty 1 MiB).
            val result = PdfDocument(PdfWriter(outputStream))
            val resultDocument = Document(result)
            asiakirjat.filter { it.tyyppi == MediaType.APPLICATION_PDF_VALUE }.forEach {
                if (!pdfContentValidator.isValid(it.asiakirjaData?.data)) {
                    throw invalidPdfAttachmentException(it)
                }
                try {
                    val sanitizedData = sanitizePdf(it.asiakirjaData?.data)
                    PdfDocument(PdfReader(ByteArrayInputStream(sanitizedData))).use { srcDoc ->
                        for (i in 1..srcDoc.numberOfPages) {
                            val page = srcDoc.getPage(i).copyTo(result)
                            result.addPage(page)
                        }
                    }
                } catch (e: Exception) {
                    throw invalidPdfAttachmentException(it, e)
                }
            }
            asiakirjat.filter { it.tyyppi == MediaType.IMAGE_JPEG_VALUE || it.tyyppi == MediaType.IMAGE_PNG_VALUE }
                .forEach {
                    try {
                        val image = Image(ImageDataFactory.create(it.asiakirjaData?.data))
                        result.addNewPage()
                        image.width = UnitValue(1, result.getPage(result.numberOfPages).pageSize.width)
                        image.setFixedPosition(result.numberOfPages, 0F, 0F)
                        image.objectFit = ObjectFit.SCALE_DOWN
                        resultDocument.add(image)
                    } catch (e: IOException) {
                        log.warn("Asiakirjan ${it.id} lisäys epäonnistui", e)
                    }
                }
            resultDocument.close()
        }
    }

    fun sanitizePdf(data: ByteArray?): ByteArray = PdfPreparation.prepare(requireNotNull(data))

    override fun yhdistaPdf(
        source: InputStream,
        newPdf: InputStream,
        outputStream: OutputStream
    ) {
        pdfMetrics.trackOperation(OP_YHDISTA_PDF) {
            // The source is the generated combined document; the incoming PDF may
            // be an uploaded certificate with copying restrictions.
            val preparedData = newPdf.use { PdfPreparation.prepare(it.readBytes()) }
            PdfDocument(PdfReader(source), PdfWriter(outputStream)).use { result ->
                PdfDocument(PdfReader(ByteArrayInputStream(preparedData))).use { newDocument ->
                    PdfMerger(result).merge(newDocument, 1, newDocument.numberOfPages)
                }
            }
        }
    }

    private fun sanitizeContent(input: String): String = PdfHtmlText.sanitize(input)
    override fun openAssembler(firstDocument: ByteArray): PdfAssembler =
        ItextPdfAssembler(firstDocument, pdfMetrics, smartMode)

    private fun isValidatedPdfTemplate(template: String): Boolean =
        template.startsWith("pdf/erikoistujantiedot/") ||
            template.endsWith("valmistumisenyhteenveto.html") ||
            template.endsWith("valmistumisenyhteenveto_yek.html") ||
            template == "pdf/koulutussopimus.html" ||
            template == "pdf/vastuuhenkilonarvio.html"

    private fun pdfSectionField(template: String): String = when {
        template == "pdf/koulutussopimus.html" -> "pdf-osio-koejakson-koulutussopimus"
        template == "pdf/vastuuhenkilonarvio.html" -> "pdf-osio-koejakson-vastuuhenkilon-arvio"
        template.endsWith("koulutussuunnitelma.html") -> "pdf-osio-koulutussuunnitelma"
        template.endsWith("paivittaisetmerkinnat.html") -> "pdf-osio-paivittaiset-merkinnat"
        template.endsWith("seurantajakso.html") -> "pdf-osio-seurantajakso"
        template.endsWith("arvioinnit.html") || template.endsWith("arviointi.html") ->
            "pdf-osio-arvioinnit"
        template.endsWith("suoritemerkinnat.html") || template.endsWith("suoritemerkinta.html") ->
            "pdf-osio-suoritemerkinnat"
        template.endsWith("valmistumisenyhteenveto.html") ||
            template.endsWith("valmistumisenyhteenveto_yek.html") ->
            "pdf-osio-valmistumisen-yhteenveto"
        else -> "arkistoitava-pdf"
    }

    private fun pdfSource(template: String): String = when {
        template == "pdf/koulutussopimus.html" -> "koejaksonkoulutussopimus"
        template == "pdf/vastuuhenkilonarvio.html" -> "koejaksonvastuuhenkilonarvio"
        template.endsWith("seurantajakso.html") -> "seurantajakso"
        else -> "valmistumispyynto"
    }


    private fun invalidPdfAttachmentException(
        asiakirja: Asiakirja,
        cause: Throwable? = null
    ) = InvalidPdfAttachmentException(
        attachmentId = asiakirja.id,
        attachmentName = asiakirja.nimi,
        source = InvalidPdfAttachmentSource.TYOSKENTELYJAKSO,
        attachmentDate = asiakirja.tyoskentelyjakso?.alkamispaiva,
        cause = cause
    )
}
