package fi.elsapalvelu.elsa.validation

import org.springframework.web.multipart.MultipartFile

interface FileValidator {
    fun validate(files: List<MultipartFile>, userId: String)
}
