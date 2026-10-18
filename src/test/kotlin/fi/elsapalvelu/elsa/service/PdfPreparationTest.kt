package fi.elsapalvelu.elsa.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PdfPreparationTest {

    @ParameterizedTest
    @ValueSource(ints = [40, 128, 256])
    fun `prepares permission-restricted PDFs without changing the original`(keyLength: Int) {
        val original = PdfTestData.certificate(openingPassword = "", keyLength = keyLength)
        val originalCopy = original.copyOf()
        Loader.loadPDF(original).use { document ->
            assertThat(document.isEncrypted).isTrue
            assertThat(document.currentAccessPermission.canExtractContent()).isFalse
            assertThat(document.currentAccessPermission.canModify()).isFalse
            assertThat(document.currentAccessPermission.canPrint()).isTrue
        }

        val prepared = PdfPreparation.prepare(original)

        assertThat(original).isEqualTo(originalCopy)
        Loader.loadPDF(prepared).use { document ->
            assertThat(document.isEncrypted).isFalse
            assertThat(document.numberOfPages).isEqualTo(1)
            assertThat(PDFTextStripper().getText(document).trim()).isEqualTo(PdfTestData.CERTIFICATE_TEXT)
        }
    }

    @Test
    fun `preserves the contents of an unencrypted PDF`() {
        val prepared = PdfPreparation.prepare(PdfTestData.certificate())

        Loader.loadPDF(prepared).use { document ->
            assertThat(document.numberOfPages).isEqualTo(1)
            assertThat(PDFTextStripper().getText(document).trim()).isEqualTo(PdfTestData.CERTIFICATE_TEXT)
        }
    }

    @Test
    fun `does not open PDFs requiring a password`() {
        val protectedPdf = PdfTestData.certificate(openingPassword = "required-password")

        assertThatThrownBy { PdfPreparation.prepare(protectedPdf) }
            .isInstanceOf(InvalidPasswordException::class.java)
    }
}
