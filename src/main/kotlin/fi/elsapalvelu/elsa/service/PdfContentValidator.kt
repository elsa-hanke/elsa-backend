package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Verifies that a PDF opens without a password and its prepared pages can be copied by iText.
 */
@Component
class PdfContentValidator {

    fun isValid(data: ByteArray?): Boolean {
        if (data == null || data.isEmpty()) {
            return false
        }

        return try {
            val preparedData = PdfPreparation.prepare(data)
            PdfDocument(PdfReader(ByteArrayInputStream(preparedData))).use { source ->
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
        } catch (_: IOException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
