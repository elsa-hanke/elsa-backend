package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.impl.FileValidationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile

class FileValidationServiceTest {

    private lateinit var asiakirjaService: AsiakirjaService
    private lateinit var fileValidationService: FileValidationService

    @BeforeEach
    fun setup() {
        asiakirjaService = mock(AsiakirjaService::class.java)
        fileValidationService = FileValidationServiceImpl(asiakirjaService)
    }

    @Test
    fun `validate should return true for valid PDF file`() {
        val file = MockMultipartFile(
            "file",
            "test.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isTrue
    }

    @Test
    fun `validate should return true for valid PNG file`() {
        val file = MockMultipartFile(
            "file",
            "image.png",
            MediaType.IMAGE_PNG_VALUE,
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isTrue
    }

    @Test
    fun `validate should return true for valid JPEG file`() {
        val file = MockMultipartFile(
            "file",
            "image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isTrue
    }

    @Test
    fun `validate should return false for invalid content type`() {
        val file = MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isFalse
    }

    @Test
    fun `validate should return false for duplicate filename`() {
        val file = MockMultipartFile(
            "file",
            "existing.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )

        val existingFile = AsiakirjaDTO(nimi = "existing.pdf")
        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(listOf(existingFile))

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isFalse
    }

    @Test
    fun `validate should return false for filename exceeding maximum length`() {
        val longName = "a".repeat(256) + ".pdf"
        val file = MockMultipartFile(
            longName,
            longName,
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file), 1L)

        assertThat(result).isFalse
    }

    @Test
    fun `validate should return true for multiple valid files`() {
        val file1 = MockMultipartFile(
            "file1",
            "test1.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )
        val file2 = MockMultipartFile(
            "file2",
            "test2.png",
            MediaType.IMAGE_PNG_VALUE,
            "content".toByteArray()
        )

        `when`(asiakirjaService.findAllByOpintooikeusId(1L)).thenReturn(emptyList())

        val result = fileValidationService.validate(listOf(file1, file2), 1L)

        assertThat(result).isTrue
    }

    @Test
    fun `validate without opintooikeusId should accept valid file types`() {
        val file = MockMultipartFile(
            "file",
            "test.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )

        val result = fileValidationService.validate(listOf(file))

        assertThat(result).isTrue
    }

    @Test
    fun `validate without opintooikeusId should reject invalid file types`() {
        val file = MockMultipartFile(
            "file",
            "test.exe",
            "application/x-msdownload",
            "content".toByteArray()
        )

        val result = fileValidationService.validate(listOf(file))

        assertThat(result).isFalse
    }

    @Test
    fun `validate should accept custom allowed content types`() {
        val file = MockMultipartFile(
            "file",
            "data.xml",
            "application/xml",
            "content".toByteArray()
        )

        val result = fileValidationService.validate(
            listOf(file),
            allowedContentTypes = listOf("application/xml")
        )

        assertThat(result).isTrue
    }

    @Test
    fun `validate should reject file not matching custom allowed content types`() {
        val file = MockMultipartFile(
            "file",
            "test.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "content".toByteArray()
        )

        val result = fileValidationService.validate(
            listOf(file),
            allowedContentTypes = listOf("application/xml")
        )

        assertThat(result).isFalse
    }
}

