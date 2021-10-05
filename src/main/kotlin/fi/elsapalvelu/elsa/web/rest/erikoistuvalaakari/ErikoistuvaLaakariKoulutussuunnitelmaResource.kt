package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKoulutussuunnitelmaResource(
    private val koulutussuunnitelmaService: KoulutussuunnitelmaService,
    private val userService: UserService,
    private val fileValidationService: FileValidationService,
) {
    companion object {
        const val ENTITY_NAME = "koulutussuunnitelma"
    }

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PutMapping("/koulutussuunnitelma")
    fun updateKoulutussuunnitelma(
        @Valid koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        @RequestParam("koulutussuunnitelmaFile") koulutussuunnitelmaFile: MultipartFile?,
        @RequestParam("motivaatiokirjeFile") motivaatiokirjeFile: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<KoulutussuunnitelmaDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (koulutussuunnitelmaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        if (koulutussuunnitelmaDTO.erikoistuvaLaakariId == null) {
            throw BadRequestAlertException("Erikoistuva lääkäri puuttuu", ENTITY_NAME, "dataillegal")
        }

        koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja = getMappedFile(koulutussuunnitelmaFile, user.id!!)
        koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja = getMappedFile(motivaatiokirjeFile, user.id!!)

        koulutussuunnitelmaService.save(koulutussuunnitelmaDTO, user.id!!)
            ?.let {
                return ResponseEntity.ok()
                    .headers(
                        HeaderUtil.createEntityUpdateAlert(
                            applicationName,
                            true,
                            ENTITY_NAME,
                            koulutussuunnitelmaDTO.id.toString()
                        )
                    )
                    .body(it)
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/koulutussuunnitelma")
    fun getKoulutussuunnitelma(
        principal: Principal?
    ): ResponseEntity<KoulutussuunnitelmaDTO> {
        val user = userService.getAuthenticatedUser(principal)

        return koulutussuunnitelmaService
            .findOneByErikoistuvaLaakariKayttajaUserId(user.id!!)?.let {
                ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    private fun getMappedFile(
        file: MultipartFile?,
        userId: String
    ): AsiakirjaDTO? {
        file?.let {
            if (!fileValidationService.validate(listOf(it), userId)) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ENTITY_NAME,
                    "illegaldata"
                )
            }

            return AsiakirjaDTO(
                nimi = file.originalFilename,
                tyyppi = file.contentType,
                asiakirjaData = AsiakirjaDataDTO(
                    fileInputStream = file.inputStream,
                    fileSize = file.size
                )
            )
        }

        return null
    }
}
