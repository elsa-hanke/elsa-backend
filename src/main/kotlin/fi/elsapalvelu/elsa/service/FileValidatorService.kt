package fi.elsapalvelu.elsa.service

import org.springframework.web.multipart.MultipartFile

interface FileValidatorService {
    fun validate(files: List<MultipartFile>, kayttajaId: String)
}
