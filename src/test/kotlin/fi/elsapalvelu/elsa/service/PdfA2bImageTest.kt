package fi.elsapalvelu.elsa.service

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream

class PdfA2bImageTest {
    private val service = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))

    @ParameterizedTest
    @CsvSource("jpeg,image/jpeg", "jpeg,image/jpg", "png,image/png")
    fun `normalizes each permitted image MIME type`(format: String, contentType: String) {
        val original = ArchiveImageTestData.image(format)
        val unchanged = original.copyOf()
        val result = service.normalize(original, contentType)
        assertThat(service.isPdfA2b(result)).isTrue()
        assertThat(original).containsExactly(*unchanged)
        withImage(result) { assertThat(it.width / it.height).isEqualTo(2F) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["jpeg", "png"])
    fun `normalizes a twelve megapixel photo within a bounded raster`(format: String) {
        val original = ArchiveImageTestData.image(format, 4000, 3000)
        val result = service.normalize(original, "image/$format")
        assertThat(service.isPdfA2b(result)).isTrue()
        withImage(result) {
            assertThat(it.width * it.height).isLessThanOrEqualTo(10_000_000F)
            assertThat(it.width / it.height).isEqualTo(4000F / 3000F)
            assertThat(it.width).isGreaterThanOrEqualTo(2000F)
        }
    }

    @Test
    fun `normalizes a direct CMYK JPEG certificate`() {
        val original = PdfTestData.cmykJpeg()
        assertThat(ImageDataFactory.create(original).colorEncodingComponentsNumber).isEqualTo(4)
        val result = service.normalize(original, "image/jpeg")
        assertThat(service.isPdfA2b(result)).isTrue()
    }

    @Test
    fun `normalizes a direct sixteen bit PNG certificate`() {
        val original = ArchiveImageTestData.sixteenBitPng()
        assertThat(original[24].toInt()).isEqualTo(16) // IHDR bit depth, not an 8-bit fixture.
        val result = service.normalize(original, "image/png")
        assertThat(service.isPdfA2b(result)).isTrue()
        withImage(result) { image ->
            val pixels = image.bufferedImage
            assertThat(pixels.getRGB(10, 20) and 0xffffff).isEqualTo(0)
            assertThat(pixels.getRGB(70, 20) and 0xffffff).isEqualTo(0xffffff)
        }
    }

    @Test
    fun `preserves JPEG camera orientation in the snapshot`() {
        val result = service.normalize(ArchiveImageTestData.orientedJpeg(6), "image/jpeg")
        withImage(result) {
            assertThat(it.width).isEqualTo(40F)
            assertThat(it.height).isEqualTo(80F)
        }
        assertThat(service.isPdfA2b(result)).isTrue()
    }

    private fun withImage(pdf: ByteArray, assertion: (PdfImageXObject) -> Unit) {
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use {
            assertThat(it.numberOfPages).isEqualTo(1)
            val images = it.firstPage.resources.getResource(PdfName.XObject)
            assertion(PdfImageXObject(images.getAsStream(images.keySet().single())))
        }
    }
}
