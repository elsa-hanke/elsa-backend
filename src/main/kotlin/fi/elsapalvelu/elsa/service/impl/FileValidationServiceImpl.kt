package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidationServiceImpl(
    private val asiakirjaService: AsiakirjaService
    ) : FileValidationService {

    private val allowedContentTypes = listOf("application/pdf", "image/jpg", "image/jpeg", "image/png")

    override fun validate(files: List<MultipartFile>, userId: String) {
        val existingFileNames = asiakirjaService.findAllByErikoistuvaLaakariUserId(userId).map { it.nimi }
        if (files.any { it.originalFilename?.toString() in existingFileNames }) {
            throw BadRequestAlertException(
                "Samanniminen tiedosto on jo olemassa",
                "asiakirja",
                "idexists"
            )
        }

        if (files.any { it.contentType?.toString() !in allowedContentTypes }) {
            throw BadRequestAlertException(
                "Sallitut tiedostomuodot: .pdf, .png, .jpeg ja .jpg",
                "asiakirja",
                "dataillegal"
            )
        }

        if (files.any { it.name.length > MAXIMUM_FILE_NAME_LENGTH }) {
            throw BadRequestAlertException(
                "Tiedostonimen maksimipituus: 255 merkkiä",
                "asiakirja",
                "dataillegal"
            )
        }
    }
}
