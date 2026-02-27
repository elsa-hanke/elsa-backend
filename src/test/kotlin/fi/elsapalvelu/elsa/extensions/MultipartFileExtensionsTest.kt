package fi.elsapalvelu.elsa.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class MultipartFileExtensionsTest {

    @Test
    fun `mapAsiakirja should convert multipart file to AsiakirjaDTO with all fields`() {
        val content = "test content".toByteArray()
        val file = MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            content
        )

        val result = file.mapAsiakirja()

        assertThat(result.nimi).isEqualTo("test-document.pdf")
        assertThat(result.tyyppi).isEqualTo("application/pdf")
        assertThat(result.asiakirjaData).isNotNull
        assertThat(result.asiakirjaData?.fileSize).isEqualTo(content.size.toLong())
        assertThat(result.asiakirjaData?.fileInputStream).isNotNull
    }

    @Test
    fun `mapAsiakirja should handle image file`() {
        val content = ByteArray(100) { it.toByte() }
        val file = MockMultipartFile(
            "image",
            "photo.jpg",
            "image/jpeg",
            content
        )

        val result = file.mapAsiakirja()

        assertThat(result.nimi).isEqualTo("photo.jpg")
        assertThat(result.tyyppi).isEqualTo("image/jpeg")
        assertThat(result.asiakirjaData?.fileSize).isEqualTo(100L)
    }

    @Test
    fun `mapAsiakirja should handle empty file`() {
        val file = MockMultipartFile(
            "file",
            "empty.pdf",
            "application/pdf",
            ByteArray(0)
        )

        val result = file.mapAsiakirja()

        assertThat(result.nimi).isEqualTo("empty.pdf")
        assertThat(result.tyyppi).isEqualTo("application/pdf")
        assertThat(result.asiakirjaData?.fileSize).isEqualTo(0L)
    }

    @Test
    fun `mapAsiakirja should handle file with special characters in name`() {
        val file = MockMultipartFile(
            "file",
            "testi-äöå-документ.pdf",
            "application/pdf",
            "content".toByteArray()
        )

        val result = file.mapAsiakirja()

        assertThat(result.nimi).isEqualTo("testi-äöå-документ.pdf")
    }

    @Test
    fun `mapAsiakirja should create readable input stream`() {
        val content = "test data".toByteArray()
        val file = MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            content
        )

        val result = file.mapAsiakirja()

        val readContent = result.asiakirjaData?.fileInputStream?.readAllBytes()
        assertThat(readContent).isEqualTo(content)
    }
}

