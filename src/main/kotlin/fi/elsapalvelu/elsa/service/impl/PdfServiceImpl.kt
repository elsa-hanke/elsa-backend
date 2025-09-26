package fi.elsapalvelu.elsa.service.impl

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
import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.service.PdfService
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
    private val templateEngine: SpringTemplateEngine
) : PdfService {

    private val log = LoggerFactory.getLogger(javaClass)

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
        val content = templateEngine.process(template, context)
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

        HtmlConverter.convertToPdf(content, pdf, properties)
    }

    override fun yhdistaAsiakirjat(
        asiakirjat: List<Asiakirja>,
        outputStream: OutputStream
    ) {
        val result = PdfDocument(PdfWriter(outputStream))
        val resultDocument = Document(result)
        val merger = PdfMerger(result)
        asiakirjat.filter { it.tyyppi == MediaType.APPLICATION_PDF_VALUE }.forEach {
            try {
                val document = PdfDocument(PdfReader(ByteArrayInputStream(it.asiakirjaData?.data)))
                merger.merge(document, 1, document.numberOfPages)
            } catch (e: IOException) {
                log.warn("Asiakirjan ${it.id} lisäys epäonnistui", e)
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

    override fun yhdistaPdf(
        source: InputStream,
        newPdf: InputStream,
        outputStream: OutputStream
    ) {
        val result = PdfDocument(PdfReader(source), PdfWriter(outputStream))
        val resultDocument = Document(result)
        val merger = PdfMerger(result)

        val newDocument = PdfDocument(PdfReader(newPdf))
        merger.merge(newDocument, 1, newDocument.numberOfPages)

        resultDocument.close()
    }
}
