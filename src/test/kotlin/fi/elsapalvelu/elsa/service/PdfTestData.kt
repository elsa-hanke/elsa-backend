package fi.elsapalvelu.elsa.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream

object PdfTestData {
    const val CERTIFICATE_TEXT = "Regression test certificate - 7 hours"

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
                content.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
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
