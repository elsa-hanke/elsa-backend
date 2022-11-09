package fi.elsapalvelu.elsa.service.impl

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.font.FontProvider
import com.itextpdf.pdfa.PdfADocument
import fi.elsapalvelu.elsa.service.PdfService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.sql.Blob

@Service
@Transactional
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

    override fun yhdistaAsiakirjat(asiakirjat: List<Blob?>, outputStream: ByteArrayOutputStream) {
        val result = PdfDocument(PdfWriter(outputStream))
        val merger = PdfMerger(result)
        asiakirjat.forEach {
            val document = PdfDocument(PdfReader(it?.binaryStream))
            merger.merge(document, 1, document.numberOfPages)
        }
        result.close()
    }
}
