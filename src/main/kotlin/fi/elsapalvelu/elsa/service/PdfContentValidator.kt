package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

enum class PdfValidationResult { VALID, PASSWORD_REQUIRED, INVALID }

/**
 * Verifies that a PDF opens without a password and its prepared pages can be copied by iText.
 */
@Component
class PdfContentValidator {

    fun isValid(data: ByteArray?): Boolean = validate(data) == PdfValidationResult.VALID

    fun validate(data: ByteArray?): PdfValidationResult {
        if (data == null || data.isEmpty()) {
            return PdfValidationResult.INVALID
        }

        return try {
            val preparedData = PdfPreparation.prepare(data)
            PdfDocument(PdfReader(ByteArrayInputStream(preparedData))).use { source ->
                if (source.reader.isEncrypted || source.numberOfPages < 1) {
                    return PdfValidationResult.INVALID
                }

                ByteArrayOutputStream().use { output ->
                    PdfDocument(PdfWriter(output)).use { target ->
                        source.copyPagesTo(1, source.numberOfPages, target)
                    }
                }
            }
            PdfValidationResult.VALID
        } catch (_: InvalidPasswordException) {
            PdfValidationResult.PASSWORD_REQUIRED
        } catch (_: IOException) {
            PdfValidationResult.INVALID
        } catch (_: RuntimeException) {
            PdfValidationResult.INVALID
        }
    }
}
