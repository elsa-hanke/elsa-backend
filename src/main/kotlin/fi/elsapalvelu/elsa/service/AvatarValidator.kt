package fi.elsapalvelu.elsa.service

import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

const val MAX_AVATAR_WIDTH = 4096
const val MAX_AVATAR_HEIGHT = 4096
const val MAX_AVATAR_PIXELS = 16_000_000L

enum class AvatarValidationResult {
    VALID,
    EMPTY,
    INVALID_CONTENT_TYPE,
    INVALID_FORMAT,
    TOO_LARGE_DIMENSIONS
}

/**
 * Validates uploaded avatar images before they are handed to Thumbnailator for processing.
 *
 * Guards against decompression-bomb style attacks by rejecting disallowed content types,
 * content whose magic bytes don't match a known safe image format, and images whose header
 * declares dimensions above the configured limits. Dimensions are read from image metadata
 * only, without decoding pixel data, so the check itself cannot be used to exhaust memory.
 */
@Component
class AvatarValidator {

    private val allowedContentTypes = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    private val jpegMagicBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val pngMagicBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun validate(data: ByteArray?, contentType: String?): AvatarValidationResult {
        if (data == null || data.isEmpty()) {
            return AvatarValidationResult.EMPTY
        }

        if (contentType == null || contentType.lowercase() !in allowedContentTypes) {
            return AvatarValidationResult.INVALID_CONTENT_TYPE
        }

        if (!hasValidMagicBytes(data)) {
            return AvatarValidationResult.INVALID_FORMAT
        }

        val dimensions = readDimensions(data) ?: return AvatarValidationResult.INVALID_FORMAT

        val (width, height) = dimensions
        if (width <= 0 || height <= 0) {
            return AvatarValidationResult.INVALID_FORMAT
        }
        if (width > MAX_AVATAR_WIDTH || height > MAX_AVATAR_HEIGHT) {
            return AvatarValidationResult.TOO_LARGE_DIMENSIONS
        }
        if (width.toLong() * height.toLong() > MAX_AVATAR_PIXELS) {
            return AvatarValidationResult.TOO_LARGE_DIMENSIONS
        }

        return AvatarValidationResult.VALID
    }

    private fun hasValidMagicBytes(data: ByteArray): Boolean {
        return startsWith(data, jpegMagicBytes) ||
            startsWith(data, pngMagicBytes) ||
            isWebp(data)
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) {
            return false
        }
        for (i in prefix.indices) {
            if (data[i] != prefix[i]) {
                return false
            }
        }
        return true
    }

    private fun isWebp(data: ByteArray): Boolean {
        if (data.size < 12) {
            return false
        }
        val riff = String(data, 0, 4, Charsets.US_ASCII)
        val webp = String(data, 8, 4, Charsets.US_ASCII)
        return riff == "RIFF" && webp == "WEBP"
    }

    /**
     * Reads only the image header (width/height) using ImageIO's metadata support,
     * without fully decoding the pixel data, so oversized images can be rejected cheaply.
     */
    private fun readDimensions(data: ByteArray): Pair<Int, Int>? {
        return try {
            MemoryCacheImageInputStream(ByteArrayInputStream(data)).use { imageInputStream ->
                val readers = ImageIO.getImageReaders(imageInputStream)
                if (!readers.hasNext()) {
                    return null
                }
                val reader = readers.next()
                try {
                    reader.setInput(imageInputStream, true, true)
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    Pair(width, height)
                } finally {
                    reader.dispose()
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
