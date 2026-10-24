package fi.elsapalvelu.elsa.service

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.awt.image.DataBuffer
import java.awt.image.Raster
import javax.imageio.IIOImage
import javax.imageio.ImageIO

object PdfTestData {
    const val CERTIFICATE_TEXT = "Regression test certificate - 7 hours"

    fun unembeddedFontPdf(pageCount: Int): ByteArray {
        val output = ByteArrayOutputStream()
        PdfDocument(PdfWriter(output)).use { document ->
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            repeat(pageCount) { index ->
                val page = document.addNewPage(PageSize.A4)
                PdfCanvas(page).beginText().setFontAndSize(font, 12F)
                    .moveText(50.0, 700.0).showText("Attachment page ${index + 1}").endText()
            }
        }
        return output.toByteArray()
    }

    /** Four unprofiled components reproduce a DeviceCMYK JPEG from an uploaded attachment. */
    fun cmykJpeg(): ByteArray {
        val raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 8, 8, 4, null)
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val output = ByteArrayOutputStream()
        try {
            ImageIO.createImageOutputStream(output).use { stream ->
                writer.output = stream
                writer.write(null, IIOImage(raster, null, null), writer.defaultWriteParam)
            }
        } finally {
            writer.dispose()
        }
        return output.toByteArray()
    }

    /** null produces an unencrypted PDF; an empty password models the reported certificate. */
    fun certificate(
        openingPassword: String? = null,
        keyLength: Int = 40,
        text: String = CERTIFICATE_TEXT
    ): ByteArray = ByteArrayOutputStream().use { output ->
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                content.beginText()
                val font = requireNotNull(javaClass.getResourceAsStream("/fonts/LiberationSerif-Regular.ttf"))
                    .use { PDType0Font.load(document, it) }
                content.setFont(font, 12f)
                content.newLineAtOffset(50f, 700f)
                content.showText(text)
                content.endText()
            }
            if (openingPassword != null) {
                val permissions = AccessPermission().apply {
                    setCanPrint(true)
                    setCanExtractContent(false)
                    setCanModify(false)
                    setCanAssembleDocument(false)
                }
                document.protect(
                    StandardProtectionPolicy("owner-password", openingPassword, permissions).apply {
                        encryptionKeyLength = keyLength
                    }
                )
            }
            document.save(output)
        }
        output.toByteArray()
    }
}
