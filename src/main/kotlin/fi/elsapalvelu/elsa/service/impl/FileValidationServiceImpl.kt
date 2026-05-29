package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.security.SecurityLoggingWrapper
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidationServiceImpl(
    private val asiakirjaService: AsiakirjaService
) : FileValidationService {

    private val log = LoggerFactory.getLogger(FileValidationServiceImpl::class.java)

    private val defaultAllowedContentTypes: List<String> =
        listOf(MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/jpg")

    override fun validate(
        files: List<MultipartFile>,
        opintooikeusId: Long,
        allowedContentTypes: List<String>?
    ): Boolean {
        val allowedContentTypesOrDefault = allowedContentTypes ?: defaultAllowedContentTypes
        val existingFileNames = asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi }
        files.forEach { file ->
            if (file.originalFilename.isNullOrBlank()) {
                log.warn("Tiedoston nimi on tyhjä.")
                return false
            }
            if (file.originalFilename!!.length > MAXIMUM_FILE_NAME_LENGTH) {
                log.warn("Opintooikeus: ${opintooikeusId} - Tiedoston nimi '${file.originalFilename}' on liian pitkä.")
                return false
            }
            if (!allowedContentTypesOrDefault.contains(file.contentType)) {
                log.warn("Opintooikeus: ${opintooikeusId} - Tiedoston '${file.originalFilename}' tyyppi '${file.contentType}' ei ole sallittu.")
                return false
            }
            if (existingFileNames.contains(file.originalFilename)) {
                log.warn("Tiedosto nimeltä '${file.originalFilename}' on jo olemassa opintooikeudella $opintooikeusId.")
                return false
            }
        }

        return true
    }

    override fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>?): Boolean {
        val allowedContentTypesOrDefault = allowedContentTypes ?: defaultAllowedContentTypes
        if (files.any {
                it.isEmpty ||
                    it.contentType?.toString() !in allowedContentTypesOrDefault ||
                    it.name.length > MAXIMUM_FILE_NAME_LENGTH
            }) {
            return false
        }

        return true
    }
}
