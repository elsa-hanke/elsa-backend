package fi.elsapalvelu.elsa.service.impl.valmistuminen

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
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextSanitizer
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_LUO_PDF
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_ASIAKIRJAT
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_PDF
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentSource
import org.apache.pdfbox.Loader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    private val pdfA2bService: PdfA2bService
) : PdfService {

    private val log = LoggerFactory.getLogger(javaClass)

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
            if (isValmistumispyyntoTemplate(template)) {
                pdfTextFieldValidator.validate(
                    fields = listOf(pdfSectionField(template) to content),
                    pdfSource = pdfSource(template)
                )
            }
            val pdf = pdfA2bService.createDocument(outputStream)
            val provider = FontProvider()
            provider.addFont(liberationSerifFont?.file?.absolutePath)
            provider.addFont(liberationSerifFontBold?.file?.absolutePath)
            provider.addFont(notoSansFont?.file?.absolutePath)
            provider.addFont(notoSansFontItalic?.file?.absolutePath)

            val properties = ConverterProperties()
            properties.fontProvider = provider

            HtmlConverter.convertToPdf(content, pdf, properties)
        }
    }

    override fun yhdistaAsiakirjat(
        asiakirjat: List<Asiakirja>,
        outputStream: OutputStream
    ) {
        pdfMetrics.trackOperation(OP_YHDISTA_ASIAKIRJAT) {
            val pdfOutput = ByteArrayOutputStream()
            val result = pdfA2bService.createDocument(pdfOutput)
            val resultDocument = Document(result)
            asiakirjat.filter { it.tyyppi == MediaType.APPLICATION_PDF_VALUE }.forEach {
                if (!pdfContentValidator.isValid(it.asiakirjaData?.data)) {
                    throw invalidPdfAttachmentException(it)
                }
                try {
                    val normalizedData = pdfA2bService.normalize(
                        sanitizePdf(it.asiakirjaData?.data),
                        MediaType.APPLICATION_PDF_VALUE
                    )
                    PdfDocument(PdfReader(ByteArrayInputStream(normalizedData))).use { srcDoc ->
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
            outputStream.write(pdfOutput.toByteArray())
        }
    }

    fun sanitizePdf(data: ByteArray?): ByteArray {
        ByteArrayOutputStream().use { out ->
            Loader.loadPDF(data).use { doc ->
                doc.isAllSecurityToBeRemoved = true
                doc.save(out)
            }
            return out.toByteArray()
        }
    }

    override fun yhdistaPdf(
        source: InputStream,
        newPdf: InputStream,
        outputStream: OutputStream
    ) {
        pdfMetrics.trackOperation(OP_YHDISTA_PDF) {
            val pdfOutput = ByteArrayOutputStream()
            val result = pdfA2bService.createDocument(pdfOutput)
            val merger = PdfMerger(result)
            listOf(source, newPdf).forEach { input ->
                val normalized = pdfA2bService.normalize(
                    input.readAllBytes(),
                    MediaType.APPLICATION_PDF_VALUE
                )
                PdfDocument(PdfReader(ByteArrayInputStream(normalized))).use { document ->
                    merger.merge(document, 1, document.numberOfPages)
                }
            }
            result.close()
            outputStream.write(pdfOutput.toByteArray())
        }
    }

    private fun sanitizeContent(input: String): String = PdfTextSanitizer.sanitize(input)

    private fun isValmistumispyyntoTemplate(template: String): Boolean =
        template.startsWith("pdf/erikoistujantiedot/") ||
            template.endsWith("valmistumisenyhteenveto.html") ||
            template.endsWith("valmistumisenyhteenveto_yek.html")

    private fun pdfSectionField(template: String): String = when {
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
