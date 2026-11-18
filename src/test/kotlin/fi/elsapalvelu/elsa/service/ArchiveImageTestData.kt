package fi.elsapalvelu.elsa.service

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.imageio.ImageIO

object ArchiveImageTestData {
    fun image(format: String = "jpeg", width: Int = 80, height: Int = 40): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color.RED
            graphics.fillRect(0, 0, width / 2, height)
            graphics.color = Color.BLUE
            graphics.fillRect(width / 2, 0, width - width / 2, height)
        } finally {
            graphics.dispose()
        }
        return encode(image, format)
    }

    fun sixteenBitPng(): ByteArray {
        val image = BufferedImage(80, 40, BufferedImage.TYPE_USHORT_GRAY)
        repeat(image.height) { y ->
            repeat(image.width) { x -> image.raster.setSample(x, y, 0, if (x < 40) 0 else 65535) }
        }
        return encode(image, "png")
    }

    fun orientedJpeg(orientation: Int, order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteArray {
        val jpeg = quadrants("jpeg")
        val exif = byteArrayOf(69, 120, 105, 102, 0, 0) + tiff(orientation, order)
        return jpeg.copyOfRange(0, 2) + byteArrayOf(0xff.toByte(), 0xe1.toByte(), 0, (exif.size + 2).toByte()) +
            exif + jpeg.copyOfRange(2, jpeg.size)
    }

    fun orientedPng(orientation: Int): ByteArray {
        val png = quadrants("png")
        val tiff = tiff(orientation, ByteOrder.BIG_ENDIAN)
        val chunk = byteArrayOf(101, 88, 73, 102) + tiff
        val encoded = ByteBuffer.allocate(chunk.size + 8).putInt(tiff.size).put(chunk)
            .putInt(CRC32().apply { update(chunk) }.value.toInt()).array()
        return png.copyOfRange(0, 33) + encoded + png.copyOfRange(33, png.size)
    }

    private fun tiff(orientation: Int, order: ByteOrder): ByteArray = ByteBuffer.allocate(26).order(order)
        .putShort(if (order == ByteOrder.LITTLE_ENDIAN) 0x4949 else 0x4d4d)
        .putShort(42).putInt(8).putShort(1)
        .putShort(0x0112).putShort(3).putInt(1).putShort(orientation.toShort()).putShort(0).putInt(0).array()

    private fun quadrants(format: String): ByteArray {
        val image = BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB)
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)
        val graphics = image.createGraphics()
        try {
            colors.forEachIndexed { index, color ->
                graphics.color = color
                graphics.fillRect(index % 2 * 40, index / 2 * 20, 40, 20)
            }
        } finally {
            graphics.dispose()
        }
        return encode(image, format)
    }

    private fun encode(image: BufferedImage, format: String): ByteArray = try {
        ByteArrayOutputStream().use {
            check(ImageIO.write(image, format, it))
            it.toByteArray()
        }
    } finally {
        image.flush()
    }
}
