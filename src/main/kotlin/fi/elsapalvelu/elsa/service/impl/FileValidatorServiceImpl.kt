package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidatorService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidatorServiceImpl(
    private val asiakirjaService: AsiakirjaService
) : FileValidatorService {

    private val allowedContentTypes = listOf("application/pdf", "image/jpg", "image/jpeg", "image/png")

    override fun validate(files: List<MultipartFile>, kayttajaId: String) {
        val existingFileNames = asiakirjaService.findAllByErikoistuvaLaakariId(kayttajaId).map { it.nimi }
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
