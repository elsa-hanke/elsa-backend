package fi.elsapalvelu.elsa.service

import org.apache.pdfbox.Loader
import java.io.ByteArrayOutputStream

/**
 * Prepares a processing copy of a PDF without changing the uploaded original.
 *
 * Some certificates open without a password but restrict copying or editing.
 * PDFBox can open these with the empty default password and remove the restrictions
 * for iText. Documents requiring an opening password still fail at loadPDF.
 */
object PdfPreparation {

    fun prepare(data: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        Loader.loadPDF(data).use { document ->
            document.isAllSecurityToBeRemoved = true
            document.save(output)
        }
        output.toByteArray()
    }
}
