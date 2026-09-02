package fi.elsapalvelu.elsa.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PdfContentValidatorTest {

    private val validator = PdfContentValidator()

    @Test
    fun `accepts a readable PDF whose pages can be copied`() {
        assertThat(validator.isValid(validPdf())).isTrue
    }

    @Test
    fun `rejects DOCX content labelled as PDF`() {
        assertThat(validator.isValid(docxContent())).isFalse
    }

    @Test
    fun `rejects arbitrary non-empty content`() {
        assertThat(validator.isValid("not a PDF".toByteArray())).isFalse
    }

    @Test
    fun `rejects a truncated PDF`() {
        assertThat(validator.isValid(validPdf().copyOf(20))).isFalse
    }

    @Test
    fun `rejects empty and null content`() {
        assertThat(validator.isValid(ByteArray(0))).isFalse
        assertThat(validator.isValid(null)).isFalse
    }

    @Test
    fun `rejects a PDF without pages`() {
        val pdfWithoutPages = ByteArrayOutputStream().use { output ->
            PDDocument().use { it.save(output) }
            output.toByteArray()
        }

        assertThat(validator.isValid(pdfWithoutPages)).isFalse
    }

    @Test
    fun `rejects a password-protected PDF`() {
        val encryptedPdf = ByteArrayOutputStream().use { output ->
            PDDocument().use { document ->
                document.addPage(PDPage())
                document.protect(
                    StandardProtectionPolicy(
                        "owner-password",
                        "user-password",
                        AccessPermission()
                    )
                )
                document.save(output)
            }
            output.toByteArray()
        }

        assertThat(validator.isValid(encryptedPdf)).isFalse
    }

    private fun validPdf(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/valid.pdf")).use { it.readBytes() }

    private fun docxContent(): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write("<Types/>".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write("<document/>".toByteArray())
            zip.closeEntry()
        }
        output.toByteArray()
    }
}
