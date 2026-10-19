package fi.elsapalvelu.elsa.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.awt.Color
import java.io.ByteArrayInputStream
import java.nio.ByteOrder
import javax.imageio.ImageIO

class ArchiveImageOrientationTest {
    @ParameterizedTest
    @CsvSource("1,0", "2,1", "3,3", "4,2", "5,0", "6,2", "7,3", "8,1")
    fun `applies all eight orientations including mirrors for JPEG and PNG`(orientation: Int, topLeftQuadrant: Int) {
        val sources = listOf(
            "image/jpeg" to ArchiveImageTestData.orientedJpeg(orientation),
            "image/jpeg" to ArchiveImageTestData.orientedJpeg(orientation, ByteOrder.BIG_ENDIAN),
            "image/png" to ArchiveImageTestData.orientedPng(orientation)
        )
        sources.forEach { (type, source) ->
            val result = ArchiveImageNormalizer().normalize(source, type)
            val image = ImageIO.read(ByteArrayInputStream(result))
            try {
                assertThat(image.width).isEqualTo(if (orientation >= 5) 40 else 80)
                assertThat(image.height).isEqualTo(if (orientation >= 5) 80 else 40)
                val expected = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)[topLeftQuadrant]
                val actual = Color(image.getRGB(10, 10))
                assertThat(actual.red).isCloseTo(expected.red, Offset.offset(10))
                assertThat(actual.green).isCloseTo(expected.green, Offset.offset(10))
                assertThat(actual.blue).isCloseTo(expected.blue, Offset.offset(10))
            } finally {
                image.flush()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 9, 65535])
    fun `ignores out of range orientation values`(orientation: Int) {
        assertThat(ArchiveImageOrientation.read(ArchiveImageTestData.orientedJpeg(orientation), "image/jpeg")).isEqualTo(1)
    }

    @Test
    fun `malformed offsets counts and truncated metadata cannot escape the segment`() {
        val valid = ArchiveImageTestData.orientedJpeg(6)
        val invalidOffset = valid.copyOf().apply { repeat(4) { this[16 + it] = 0xff.toByte() } }
        val invalidCount = valid.copyOf().apply { this[20] = 0xff.toByte(); this[21] = 0xff.toByte() }
        listOf(invalidOffset, invalidCount, valid.copyOf(16)).forEach {
            assertThat(ArchiveImageOrientation.read(it, "image/jpeg")).isEqualTo(1)
        }
        val png = ArchiveImageTestData.orientedPng(6)
        repeat(4) { png[33 + it] = 0xff.toByte() }
        assertThat(ArchiveImageOrientation.read(png, "image/png")).isEqualTo(1)
    }
}
