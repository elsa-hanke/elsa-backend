package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private const val MAXIMUM_FILE_NAME_LENGTH = 255

@Service
class FileValidationServiceImpl(
    private val asiakirjaService: AsiakirjaService,
    private val pdfContentValidator: PdfContentValidator
) : FileValidationService {

    private val log = LoggerFactory.getLogger(javaClass)

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
            val contentType = file.contentType
            if (file.originalFilename.isNullOrBlank()) {
                log.warn("Tiedoston nimi on tyhjä.")
                return false
            }
            if (file.originalFilename.required().length > MAXIMUM_FILE_NAME_LENGTH) {
                log.warn("Opintooikeus: $opintooikeusId - Tiedoston nimi '${file.originalFilename}' on liian pitkä.")
                return false
            }
            if (contentType == null || contentType !in allowedContentTypesOrDefault) {
                log.warn("Opintooikeus: $opintooikeusId - Tiedoston '${file.originalFilename}' tyyppi '$contentType' ei ole sallittu.")
                return false
            }
            if (existingFileNames.contains(file.originalFilename)) {
                log.warn("Tiedosto nimeltä '${file.originalFilename}' on jo olemassa opintooikeudella $opintooikeusId.")
                return false
            }
            if (file.isEmpty) {
                log.warn("Tiedosto  '${file.originalFilename}'  on tyhjä opintooikeudella $opintooikeusId.")
                return false
            }
            if (!hasValidPdfContent(file, opintooikeusId)) {
                return false
            }
        }

        return true
    }

    override fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>?): Boolean {
        val allowedContentTypesOrDefault = allowedContentTypes ?: defaultAllowedContentTypes
        return !files.any {
            it.isEmpty ||
                (it.contentType ?: "") !in allowedContentTypesOrDefault ||
                it.name.length > MAXIMUM_FILE_NAME_LENGTH ||
                !hasValidPdfContent(it)
        }
    }

    private fun hasValidPdfContent(file: MultipartFile, opintooikeusId: Long? = null): Boolean {
        if (file.contentType != MediaType.APPLICATION_PDF_VALUE) {
            return true
        }

        val valid = try {
            pdfContentValidator.isValid(file.bytes)
        } catch (_: java.io.IOException) {
            false
        }

        if (!valid) {
            val opintooikeus = opintooikeusId?.let { "Opintooikeus: $it - " }.orEmpty()
            log.warn(
                "${opintooikeus}Tiedosto '${file.originalFilename}' ei sisällä kelvollista PDF-dataa."
            )
        }
        return valid
    }
}
