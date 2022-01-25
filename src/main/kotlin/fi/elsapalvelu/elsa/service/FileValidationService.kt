package fi.elsapalvelu.elsa.service

import org.springframework.web.multipart.MultipartFile

interface FileValidationService {
    fun validate(files: List<MultipartFile>, opintooikeusId: Long, allowedContentTypes: List<String>? = null): Boolean

    fun validate(files: List<MultipartFile>, allowedContentTypes: List<String>? = null): Boolean
}
