package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import org.springframework.web.multipart.MultipartFile

fun MultipartFile.mapAsiakirja(): AsiakirjaDTO {
    return AsiakirjaDTO(
        nimi = this.originalFilename,
        tyyppi = this.contentType,
        asiakirjaData = AsiakirjaDataDTO(
            fileInputStream = this.inputStream,
            fileSize = this.size
        )
    )
}
