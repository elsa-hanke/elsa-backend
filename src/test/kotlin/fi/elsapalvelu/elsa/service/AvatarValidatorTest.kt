package fi.elsapalvelu.elsa.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.imageio.ImageIO

class AvatarValidatorTest {

    private val validator = AvatarValidator()

    @Test
    fun `accepts a valid small JPEG`() {
        val bytes = image(10, 10, "jpg")
        assertThat(validator.validate(bytes, "image/jpeg")).isEqualTo(AvatarValidationResult.VALID)
    }

    @Test
    fun `accepts a valid small PNG`() {
        val bytes = image(10, 10, "png")
        assertThat(validator.validate(bytes, "image/png")).isEqualTo(AvatarValidationResult.VALID)
    }

    @Test
    fun `rejects null content`() {
        assertThat(validator.validate(null, "image/png")).isEqualTo(AvatarValidationResult.EMPTY)
    }

    @Test
    fun `rejects empty content`() {
        assertThat(validator.validate(ByteArray(0), "image/png")).isEqualTo(AvatarValidationResult.EMPTY)
    }

    @Test
    fun `rejects disallowed content type`() {
        val bytes = image(10, 10, "png")
        assertThat(validator.validate(bytes, "image/gif")).isEqualTo(AvatarValidationResult.INVALID_CONTENT_TYPE)
        assertThat(
            validator.validate(bytes, "application/octet-stream")
        ).isEqualTo(AvatarValidationResult.INVALID_CONTENT_TYPE)
        assertThat(validator.validate(bytes, null)).isEqualTo(AvatarValidationResult.INVALID_CONTENT_TYPE)
    }

    @Test
    fun `rejects content whose magic bytes do not match a real image, even with an allowed content type`() {
        val pdfLikeBytes = "%PDF-1.4 not really a pdf either".toByteArray()
        assertThat(validator.validate(pdfLikeBytes, "image/png")).isEqualTo(AvatarValidationResult.INVALID_FORMAT)
    }

    @Test
    fun `rejects truncated image content`() {
        val bytes = image(10, 10, "png").copyOf(5)
        assertThat(validator.validate(bytes, "image/png")).isEqualTo(AvatarValidationResult.INVALID_FORMAT)
    }

    @Test
    fun `rejects an image whose header declares dimensions above the configured maximum`() {
        val bombBytes = pngDecompressionBomb(width = 50_000, height = 50_000)
        assertThat(
            validator.validate(bombBytes, "image/png")
        ).isEqualTo(AvatarValidationResult.TOO_LARGE_DIMENSIONS)
    }

    @Test
    fun `rejects an image whose declared pixel count exceeds the maximum even within width and height limits`() {
        // Just under the width/height caps individually, but the product exceeds MAX_AVATAR_PIXELS.
        val bombBytes = pngDecompressionBomb(width = MAX_AVATAR_WIDTH, height = MAX_AVATAR_HEIGHT)
        assertThat(
            validator.validate(bombBytes, "image/png")
        ).isEqualTo(AvatarValidationResult.TOO_LARGE_DIMENSIONS)
    }

    @Test
    fun `accepts an image at exactly the maximum allowed dimensions when pixel count is within limits`() {
        val bytes = image(16, 16, "png")
        assertThat(validator.validate(bytes, "image/png")).isEqualTo(AvatarValidationResult.VALID)
    }

    private fun image(width: Int, height: Int, format: String): ByteArray {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, format, output)
        return output.toByteArray()
    }

    /**
     * Builds a real, tiny, valid PNG (small pixel payload) but patches the IHDR chunk to
     * declare the given (potentially huge) width/height, recalculating the chunk's CRC.
     * This mimics a decompression-bomb file: cheap to transmit/store, but claiming an
     * enormous decoded size. AvatarValidator must reject it based on header metadata alone,
     * without ever decoding the pixel data.
     */
    private fun pngDecompressionBomb(width: Int, height: Int): ByteArray {
        val original = image(1, 1, "png")

        // PNG layout: 8-byte signature, then chunks of [length(4)][type(4)][data(length)][crc(4)].
        // The IHDR chunk is always first and its data begins with width(4) then height(4).
        val ihdrDataStart = 8 + 4 + 4
        val patched = original.copyOf()

        writeIntBigEndian(patched, ihdrDataStart, width)
        writeIntBigEndian(patched, ihdrDataStart + 4, height)

        val ihdrLength = readIntBigEndian(patched, 8)
        val crc = CRC32()
        // CRC covers chunk type + chunk data (not the length field).
        crc.update(patched, 12, 4 + ihdrLength)
        writeIntBigEndian(patched, 8 + 4 + 4 + ihdrLength, crc.value.toInt())

        return patched
    }

    private fun writeIntBigEndian(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }

    private fun readIntBigEndian(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }
}
