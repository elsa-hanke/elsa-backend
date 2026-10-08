package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfValidationResult
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
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
    private val defaultAllowedContentTypes =
        listOf(MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/jpg")

    override fun validate(files: List<MultipartFile>, opintooikeusId: Long, allowedContentTypes: List<String>?) {
        val existingFileNames = asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi }
        files.forEach { file ->
            validateFilename(file.originalFilename)
            validateContentType(file.originalFilename, file.contentType, allowedContentTypes)
            if (file.originalFilename in existingFileNames) {
                reject(file.originalFilename, "samanniminen-tiedosto-on-jo-olemassa", "Samanniminen tiedosto on jo olemassa.")
            }
            validateContent(file.originalFilename, file.contentType, file.isEmpty) { file.bytes }
        }
    }

    override fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>?) {
        files.forEach { file ->
            validateFilename(file.originalFilename)
            validateContentType(file.originalFilename, file.contentType, allowedContentTypes)
            validateContent(file.originalFilename, file.contentType, file.isEmpty) { file.bytes }
        }
    }

    override fun validate(data: ByteArray, originalFilename: String?, contentType: String?) {
        validateFilename(originalFilename)
        validateContentType(originalFilename, contentType, null)
        validateContent(originalFilename, contentType, data.isEmpty()) { data }
    }

    private fun validateFilename(originalFilename: String?) {
        if (originalFilename.isNullOrBlank() || originalFilename.length > MAXIMUM_FILE_NAME_LENGTH) {
            reject(originalFilename, "tiedosto-ei-ole-kelvollinen", "Tarkista tiedoston nimi.")
        }
    }

    private fun validateContentType(originalFilename: String?, contentType: String?, allowedContentTypes: List<String>?) {
        if (contentType == null || contentType !in (allowedContentTypes ?: defaultAllowedContentTypes)) {
            reject(originalFilename, "tiedostotyyppi-ei-ole-sallittu", "Tiedostomuoto ei ole sallittu.")
        }
    }

    private fun validateContent(originalFilename: String?, contentType: String?, empty: Boolean, data: () -> ByteArray) {
        if (empty) {
            reject(originalFilename, "tiedosto-on-tyhja", "Liitetiedosto on tyhjä.")
        }
        if (contentType != MediaType.APPLICATION_PDF_VALUE) return

        val result = try {
            pdfContentValidator.validate(data())
        } catch (_: java.io.IOException) {
            PdfValidationResult.INVALID
        }
        when (result) {
            PdfValidationResult.VALID -> Unit
            PdfValidationResult.PASSWORD_REQUIRED ->
                reject(originalFilename, "pdf-tiedosto-vaatii-salasanan", "PDF-tiedosto vaatii salasanan avaamiseen.")
            PdfValidationResult.INVALID ->
                reject(originalFilename, "pdf-tiedostoa-ei-voitu-kasitella", "Liitetiedostoa ei voitu käsitellä.")
        }
    }

    private fun reject(originalFilename: String?, errorKey: String, message: String): Nothing {
        log.warn("Tiedoston '{}' validointi epäonnistui: {}", originalFilename, errorKey)
        throw BadRequestAlertException(message, "asiakirja", "dataillegal.$errorKey")
    }
}
