package fi.elsapalvelu.elsa.service

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads only the bounded first TIFF IFD's orientation, without decoding arbitrary metadata. */
internal object ArchiveImageOrientation {
    fun read(data: ByteArray, contentType: String): Int = when (contentType) {
        "image/jpeg" -> jpegOrientation(data)
        "image/png" -> pngOrientation(data)
        else -> 1
    }

    private fun jpegOrientation(data: ByteArray): Int {
        var offset = 2 // SOI; format has already been checked by ImageIO.
        while (offset + 4 <= data.size && unsigned(data[offset]) == 0xff) {
            val marker = unsigned(data[offset + 1])
            if (marker == 0xff) {
                offset++
                continue
            }
            if (marker == 0xda || marker == 0xd9) return 1 // SOS/EOI: no metadata after image data.
            val length = unsigned(data[offset + 2]) * 256 + unsigned(data[offset + 3])
            if (length < 2 || offset.toLong() + 2 + length > data.size) return 1
            if (marker == 0xe1 && length >= 8 && EXIF.indices.all { data[offset + 4 + it] == EXIF[it] }) {
                return tiffOrientation(data, offset + 10, length - 8)
            }
            offset += 2 + length
        }
        return 1
    }

    private fun pngOrientation(data: ByteArray): Int {
        var offset = 8 // PNG signature, already checked by ImageIO.
        val buffer = ByteBuffer.wrap(data)
        while (offset.toLong() + 12 <= data.size) {
            val length = buffer.getInt(offset).toLong() and 0xffffffffL
            if (offset + 12L + length > data.size) break
            if (buffer.getInt(offset + 4) == PNG_EXIF) return tiffOrientation(data, offset + 8, length.toInt())
            offset += 12 + length.toInt()
        }
        return 1
    }

    private fun tiffOrientation(data: ByteArray, start: Int, size: Int): Int {
        if (size < 8) return 1
        val buffer = ByteBuffer.wrap(data, start, size).slice()
        buffer.order(when (buffer.getShort(0).toInt()) {
            0x4949 -> ByteOrder.LITTLE_ENDIAN
            0x4d4d -> ByteOrder.BIG_ENDIAN
            else -> return 1
        })
        if (buffer.getShort(2).toInt() != 42) return 1
        val ifd = buffer.getInt(4).toLong() and 0xffffffffL
        if (ifd < 8 || ifd + 2 > size) return 1
        val entries = buffer.getShort(ifd.toInt()).toInt() and 0xffff
        if (ifd + 2 + entries.toLong() * 12 > size) return 1
        repeat(entries) { index ->
            val entry = ifd.toInt() + 2 + index * 12
            if (buffer.getShort(entry).toInt() == 0x0112 && buffer.getShort(entry + 2).toInt() == 3 &&
                buffer.getInt(entry + 4) == 1) {
                return buffer.getShort(entry + 8).toInt().takeIf { it in 1..8 } ?: 1
            }
        }
        return 1
    }

    private fun unsigned(value: Byte): Int = value.toInt() and 0xff

    private val EXIF = byteArrayOf(69, 120, 105, 102, 0, 0)
    private const val PNG_EXIF = 0x65584966
}
