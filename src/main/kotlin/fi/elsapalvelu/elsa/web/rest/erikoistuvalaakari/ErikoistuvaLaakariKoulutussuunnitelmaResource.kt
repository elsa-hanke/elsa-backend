package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
    private val opintooikeusService: OpintooikeusService
) {
    companion object {
        const val ENTITY_NAME = "koulutussuunnitelma"
    }

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PutMapping("/koulutussuunnitelma")
    fun updateKoulutussuunnitelma(
        @Valid koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        @RequestParam koulutussuunnitelmaFile: MultipartFile?,
        @RequestParam motivaatiokirjeFile: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<KoulutussuunnitelmaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (koulutussuunnitelmaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja = getMappedFile(koulutussuunnitelmaFile, opintooikeusId)
        koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja = getMappedFile(motivaatiokirjeFile, opintooikeusId)

        koulutussuunnitelmaService.save(koulutussuunnitelmaDTO, opintooikeusId)
            ?.let {
                return ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/koulutussuunnitelma")
    fun getKoulutussuunnitelma(
        principal: Principal?
    ): ResponseEntity<KoulutussuunnitelmaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        return koulutussuunnitelmaService
            .findOneByOpintooikeusId(opintooikeusId)?.let {
                ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    private fun getMappedFile(
        file: MultipartFile?,
        opintooikeusId: Long
    ): AsiakirjaDTO? {
        file?.let {
            if (!fileValidationService.validate(listOf(it), opintooikeusId, listOf(MediaType.APPLICATION_PDF_VALUE))) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return file.mapAsiakirja()
        }

        return null
    }
}
