package fi.elsapalvelu.elsa.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader

class ArchiveImageNormalizerTest {
    @Test
    fun `subsampling is passed to the reader before raster decoding`() {
        val reader = mockReader(4000, 3000)
        whenever(reader.defaultReadParam).thenReturn(ImageReadParam())
        whenever(reader.read(eq(0), any<ImageReadParam>())).thenAnswer {
            val parameters = it.getArgument<ImageReadParam>(1)
            assertThat(parameters.sourceXSubsampling).isEqualTo(2)
            assertThat(parameters.sourceYSubsampling).isEqualTo(2)
            BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB)
        }

        val result = ArchiveImageNormalizer { reader }.normalize(byteArrayOf(1), "image/jpeg")

        ImageIO.read(ByteArrayInputStream(result)).let {
            assertThat(it.width).isEqualTo(2000)
            assertThat(it.height).isEqualTo(1500)
            it.flush()
        }
        verify(reader).dispose()
    }

    @ParameterizedTest
    @CsvSource("0,100", "-1,100", "2147483647,2147483647", "16385,1", "8000,8000", "6000,6000", "6000,5000")
    fun `rejects dangerous source dimensions or memory estimate before decoding`(width: Int, height: Int) {
        val reader = mockReader(width, height)

        assertThrows<IllegalArgumentException> {
            ArchiveImageNormalizer { reader }.normalize(byteArrayOf(1), "image/jpeg")
        }

        verify(reader, never()).read(any(), any<ImageReadParam>())
        verify(reader).dispose()
    }

    @Test
    fun `rejects excessive compressed input before selecting a decoder`() {
        var selected = false
        val normalizer = ArchiveImageNormalizer {
            selected = true
            error("Must not select decoder")
        }
        assertThrows<IllegalArgumentException> { normalizer.normalize(ByteArray(20 * 1024 * 1024 + 1), "image/png") }
        assertThat(selected).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["jpeg", "png"])
    fun `rejects truncated image instead of producing a partial certificate`(format: String) {
        val original = ArchiveImageTestData.image(format)
        assertThrows<IllegalArgumentException> {
            ArchiveImageNormalizer().normalize(original.copyOf(original.size / 2), "image/$format")
        }
    }

    @Test
    fun `rejects a supported image with a mismatched MIME type`() {
        assertThrows<IllegalArgumentException> {
            ArchiveImageNormalizer().normalize(ArchiveImageTestData.image("png"), "image/jpeg")
        }
    }

    @Test
    fun `rejects legacy JPEG fixture missing an AC Huffman table`() {
        // Despite its filename, this fixture is not decodable: old embedding only inspected headers.
        val source = requireNotNull(javaClass.getResourceAsStream("/fixtures/valid.jpg")).use { it.readBytes() }
        assertThrows<IllegalArgumentException> { ArchiveImageNormalizer().normalize(source, "image/jpeg") }
    }

    @Test
    fun `does not accept a GIF disguised as PNG`() {
        assertThrows<IllegalArgumentException> {
            ArchiveImageNormalizer().normalize(ArchiveImageTestData.image("gif"), "image/png")
        }
    }

    @Test
    fun `flattens transparent PNG onto a white document background`() {
        val source = BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)
        source.setRGB(10, 10, Color.BLACK.rgb)
        val encoded = ByteArrayOutputStream().also { check(ImageIO.write(source, "png", it)) }.toByteArray()
        source.flush()
        val normalized = ArchiveImageNormalizer().normalize(encoded, "image/png")
        val result = ImageIO.read(ByteArrayInputStream(normalized))
        try {
            assertThat(result.getRGB(0, 0)).isEqualTo(Color.WHITE.rgb)
            assertThat(result.getRGB(10, 10)).isEqualTo(Color.BLACK.rgb)
            assertThat(result.colorModel.hasAlpha()).isFalse()
        } finally {
            result.flush()
        }
    }

    private fun mockReader(width: Int, height: Int): ImageReader = mock<ImageReader>().apply {
        whenever(formatName).thenReturn("JPEG")
        whenever(getWidth(0)).thenReturn(width)
        whenever(getHeight(0)).thenReturn(height)
    }
}
