package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidationServiceImpl(
    private val asiakirjaService: AsiakirjaService
) : FileValidationService {

    private val defaultAllowedContentTypes: List<String> =
        listOf(MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/jpg")

    override fun validate(
        files: List<MultipartFile>,
        opintooikeusId: Long,
        allowedContentTypes: List<String>?
    ): Boolean {
        val allowedContentTypesOrDefault = allowedContentTypes ?: defaultAllowedContentTypes
        val existingFileNames = asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi }
        if (files.any {
                it.originalFilename?.toString() in existingFileNames ||
                    it.contentType?.toString() !in allowedContentTypesOrDefault ||
                    it.name.length > MAXIMUM_FILE_NAME_LENGTH
            }) {
            return false
        }

        return true
    }

    override fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>?): Boolean {
        val allowedContentTypesOrDefault = allowedContentTypes ?: defaultAllowedContentTypes
        if (files.any {
                it.contentType?.toString() !in allowedContentTypesOrDefault ||
                    it.name.length > MAXIMUM_FILE_NAME_LENGTH
            }) {
            return false
        }

        return true
    }
}
