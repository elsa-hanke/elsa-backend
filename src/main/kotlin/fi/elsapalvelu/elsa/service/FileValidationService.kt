package fi.elsapalvelu.elsa.service

import org.springframework.web.multipart.MultipartFile

interface FileValidationService {
    fun validate(files: List<MultipartFile>, userId: String)
}
