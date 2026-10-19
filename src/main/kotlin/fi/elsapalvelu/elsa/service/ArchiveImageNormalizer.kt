package fi.elsapalvelu.elsa.service

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.MemoryCacheImageInputStream

/** Bounded image decoding for archive copies only; ordinary PDF merging keeps its original path. */
internal class ArchiveImageNormalizer(
    private val readerProvider: (ImageInputStream) -> ImageReader = { input ->
        val readers = ImageIO.getImageReaders(input)
        require(readers.hasNext()) { "Unsupported archive image" }
        readers.next()
    }
) {
    fun normalize(data: ByteArray, contentType: String): ByteArray {
        require(data.isNotEmpty() && data.size <= MAX_INPUT_BYTES) { "Archive image exceeds the input limit" }
        try {
            MemoryCacheImageInputStream(ByteArrayInputStream(data)).use { input ->
                val reader = readerProvider(input)
                try {
                    reader.setInput(input, true, true)
                    val format = reader.formatName.lowercase()
                    require((format == "jpeg" && contentType == "image/jpeg") ||
                        (format == "png" && contentType == "image/png")) { "Archive image type does not match its content" }
                    val parameters = readParameters(reader, data.size)
                    var decodingWarning = false
                    reader.addIIOReadWarningListener { _, _ -> decodingWarning = true }
                    val decoded = reader.read(0, parameters)
                    try {
                        require(!decodingWarning) { "Archive image decoder reported damaged or unsupported content" }
                        require(decoded.width.toLong() * decoded.height <= MAX_DECODED_PIXELS &&
                            maxOf(decoded.width, decoded.height) <= MAX_DECODED_DIMENSION) { "Archive image decoder exceeded limits" }
                        return toRgbPng(decoded, ArchiveImageOrientation.read(data, contentType))
                    } finally {
                        decoded.flush()
                    }
                } finally {
                    reader.dispose()
                }
            }
        } catch (exception: IOException) {
            throw IllegalArgumentException("Archive image cannot be decoded", exception)
        }
    }

    private fun readParameters(reader: ImageReader, inputBytes: Int): ImageReadParam {
        val width = reader.getWidth(0)
        val height = reader.getHeight(0)
        val sourcePixels = width.toLong() * height
        require(width in 1..MAX_SOURCE_DIMENSION && height in 1..MAX_SOURCE_DIMENSION && sourcePixels <= MAX_SOURCE_PIXELS) {
            "Archive image exceeds source dimensions"
        }
        // Admission estimate, not a JVM heap guarantee: reserve decoder/source buffers, compressed
        // copies, decoded raster, RGB conversion, PNG encoding and subsequent PDF embedding.
        val available = WORKING_BYTES - sourcePixels * SOURCE_BYTES_PER_PIXEL - inputBytes.toLong() * 2
        val pixelBudget = minOf(MAX_DECODED_PIXELS, available / DECODED_BYTES_PER_PIXEL)
        require(pixelBudget > 0) { "Archive image exceeds working memory budget" }
        var subsampling = 1
        while (sampledSize(width, subsampling) * sampledSize(height, subsampling) > pixelBudget ||
            sampledSize(maxOf(width, height), subsampling) > MAX_DECODED_DIMENSION) {
            subsampling++
            require(subsampling <= MAX_SUBSAMPLING) { "Archive image requires excessive downsampling" }
        }
        // Applied by the decoder, before allocating the output raster; never decode then resize.
        return reader.defaultReadParam.apply { setSourceSubsampling(subsampling, subsampling, 0, 0) }
    }

    private fun sampledSize(size: Int, subsampling: Int): Long = (size.toLong() + subsampling - 1) / subsampling

    private fun toRgbPng(source: BufferedImage, orientation: Int): ByteArray {
        val swapped = orientation in 5..8
        val target = BufferedImage(
            if (swapped) source.height else source.width,
            if (swapped) source.width else source.height,
            BufferedImage.TYPE_INT_RGB
        )
        try {
            val graphics = target.createGraphics()
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, target.width, target.height)
                // The decoded ColorModel performs CMYK/16-bit to sRGB conversion. Flatten alpha
                // on a white document background; only the immutable archive copy is changed.
                graphics.drawImage(source, transform(orientation, source.width.toDouble(), source.height.toDouble()), null)
            } finally {
                graphics.dispose()
            }
            return ByteArrayOutputStream().use { output ->
                check(ImageIO.write(target, "png", output)) { "PNG encoder unavailable" }
                output.toByteArray()
            }
        } finally {
            target.flush()
        }
    }

    private fun transform(orientation: Int, width: Double, height: Double): AffineTransform = when (orientation) {
        2 -> AffineTransform(-1.0, 0.0, 0.0, 1.0, width, 0.0)
        3 -> AffineTransform(-1.0, 0.0, 0.0, -1.0, width, height)
        4 -> AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, height)
        5 -> AffineTransform(0.0, 1.0, 1.0, 0.0, 0.0, 0.0)
        6 -> AffineTransform(0.0, 1.0, -1.0, 0.0, height, 0.0)
        7 -> AffineTransform(0.0, -1.0, -1.0, 0.0, height, width)
        8 -> AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, width)
        else -> AffineTransform()
    }

    private companion object {
        const val MAX_INPUT_BYTES = 20 * 1024 * 1024
        const val MAX_SOURCE_DIMENSION = 16_384
        const val MAX_SOURCE_PIXELS = 40_000_000L
        const val MAX_DECODED_DIMENSION = 4096
        const val MAX_DECODED_PIXELS = 10_000_000L
        const val MAX_SUBSAMPLING = 4
        const val WORKING_BYTES = 256L * 1024 * 1024
        const val SOURCE_BYTES_PER_PIXEL = 8
        const val DECODED_BYTES_PER_PIXEL = 32
    }
}
