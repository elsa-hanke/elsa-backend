package fi.elsapalvelu.elsa.service.impl

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
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
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

@Service
class PdfServiceImpl(
    private val templateEngine: SpringTemplateEngine
) : PdfService {

    override fun luoPdf(template: String, context: Context, outputStream: ByteArrayOutputStream) {
        val content = templateEngine.process(template, context)
        val pdf = PdfADocument(
            PdfWriter(outputStream),
            PdfAConformanceLevel.PDF_A_2B,
            PdfOutputIntent(
                "Custom", "", "https://www.color.org",
                "sRGB IEC61966-2.1", FileInputStream("src/main/resources/sRGB_CS_profile.icm")
            )
        )
        val provider = FontProvider()
        provider.addFont("src/main/resources/times-bold.ttf")
        provider.addFont("src/main/resources/times-roman.ttf")

        val properties = ConverterProperties()
        properties.fontProvider = provider

        HtmlConverter.convertToPdf(content, pdf, properties)
    }

    override fun yhdistaAsiakirjat(
        asiakirjat: List<Asiakirja>,
        outputStream: ByteArrayOutputStream
    ) {
        val result = PdfDocument(PdfWriter(outputStream))
        val resultDocument = Document(result)
        val merger = PdfMerger(result)
        asiakirjat.filter { it.tyyppi == MediaType.APPLICATION_PDF_VALUE }.forEach {
            val document = PdfDocument(PdfReader(it.asiakirjaData?.data?.binaryStream))
            merger.merge(document, 1, document.numberOfPages)
        }
        asiakirjat.filter { it.tyyppi == MediaType.IMAGE_JPEG_VALUE || it.tyyppi == MediaType.IMAGE_PNG_VALUE }
            .forEach {
                result.addNewPage()
                val image =
                    Image(ImageDataFactory.create(it.asiakirjaData?.data?.binaryStream?.readAllBytes()))
                image.width = UnitValue(1, result.getPage(result.numberOfPages).pageSize.width)
                image.setFixedPosition(result.numberOfPages, 0F, 0F)
                image.objectFit = ObjectFit.SCALE_DOWN
                resultDocument.add(image)
            }
        resultDocument.close()
    }
}
