package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Verifies that bytes are a readable, non-encrypted PDF whose pages iText can copy.
 */
@Component
class PdfContentValidator {

    fun isValid(data: ByteArray?): Boolean {
        if (data == null || data.isEmpty()) {
            return false
        }

        return try {
            PdfDocument(PdfReader(ByteArrayInputStream(data))).use { source ->
                if (source.reader.isEncrypted || source.numberOfPages < 1) {
                    return false
                }

                ByteArrayOutputStream().use { output ->
                    PdfDocument(PdfWriter(output)).use { target ->
                        source.copyPagesTo(1, source.numberOfPages, target)
                    }
                }
            }
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}
