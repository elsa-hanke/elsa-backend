package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidationServiceImpl(
    private val asiakirjaService: AsiakirjaService
) : FileValidationService {

    private val allowedContentTypes = listOf("application/pdf", "image/jpg", "image/jpeg", "image/png")

    override fun validate(files: List<MultipartFile>, userId: String): Boolean {
        val existingFileNames = asiakirjaService.findAllByErikoistuvaLaakariUserId(userId).map { it.nimi }
        if (files.any {
                it.originalFilename?.toString() in existingFileNames ||
                    it.contentType?.toString() !in allowedContentTypes ||
                    it.name.length > MAXIMUM_FILE_NAME_LENGTH
            }) {
            return false
        }

        return true
    }
}
