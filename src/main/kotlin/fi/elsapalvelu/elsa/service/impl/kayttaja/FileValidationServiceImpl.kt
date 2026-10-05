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
            if (file.originalFilename.isNullOrBlank() || file.originalFilename!!.length > MAXIMUM_FILE_NAME_LENGTH) {
                reject(file, "tiedosto-ei-ole-kelvollinen", "Tarkista tiedoston nimi.")
            }
            validateContentType(file, allowedContentTypes)
            if (file.originalFilename in existingFileNames) {
                reject(file, "samanniminen-tiedosto-on-jo-olemassa", "Samanniminen tiedosto on jo olemassa.")
            }
            validateContent(file)
        }
    }

    override fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>?) {
        files.forEach { file ->
            if (file.name.length > MAXIMUM_FILE_NAME_LENGTH) {
                reject(file, "tiedosto-ei-ole-kelvollinen", "Tarkista tiedoston nimi.")
            }
            validateContentType(file, allowedContentTypes)
            validateContent(file)
        }
    }

    private fun validateContentType(file: MultipartFile, allowedContentTypes: List<String>?) {
        val contentType = file.contentType
        if (contentType == null || contentType !in (allowedContentTypes ?: defaultAllowedContentTypes)) {
            reject(file, "tiedostotyyppi-ei-ole-sallittu", "Tiedostomuoto ei ole sallittu.")
        }
    }

    private fun validateContent(file: MultipartFile) {
        if (file.isEmpty) {
            reject(file, "tiedosto-on-tyhja", "Liitetiedosto on tyhjä.")
        }
        if (file.contentType != MediaType.APPLICATION_PDF_VALUE) return

        val result = try {
            pdfContentValidator.validate(file.bytes)
        } catch (_: java.io.IOException) {
            PdfValidationResult.INVALID
        }
        when (result) {
            PdfValidationResult.VALID -> Unit
            PdfValidationResult.PASSWORD_REQUIRED ->
                reject(file, "pdf-tiedosto-vaatii-salasanan", "PDF-tiedosto vaatii salasanan avaamiseen.")
            PdfValidationResult.INVALID ->
                reject(file, "pdf-tiedostoa-ei-voitu-kasitella", "Liitetiedostoa ei voitu käsitellä.")
        }
    }

    private fun reject(file: MultipartFile, errorKey: String, message: String): Nothing {
        log.warn("Tiedoston '{}' validointi epäonnistui: {}", file.originalFilename, errorKey)
        throw BadRequestAlertException(message, "asiakirja", "dataillegal.$errorKey")
    }
}
