package fi.elsapalvelu.elsa.service.kayttaja

import org.springframework.web.multipart.MultipartFile

/** Rejects an invalid upload with a reason-specific BadRequestAlertException. */
interface FileValidationService {
    fun validate(files: List<MultipartFile>, opintooikeusId: Long, allowedContentTypes: List<String>? = null)

    fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>? = null)
}
