package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import jakarta.validation.Valid

@RestController
@PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
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
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (koulutussuunnitelmaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja =
            getMappedFile(koulutussuunnitelmaFile, opintooikeusId)
        koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja =
            getMappedFile(motivaatiokirjeFile, opintooikeusId)

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
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        return koulutussuunnitelmaService
            .findOneByOpintooikeusId(opintooikeusId)?.let {
                if ((principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
                        .contains(ERIKOISTUVA_LAAKARI_IMPERSONATED)
                ) {
                    if (it.opiskeluJaTyohistoriaYksityinen == true) {
                        it.opiskeluJaTyohistoria = null
                    }
                    if (it.vahvuudetYksityinen == true) {
                        it.vahvuudet = null
                    }
                    if (it.tulevaisuudenVisiointiYksityinen == true) {
                        it.tulevaisuudenVisiointi = null
                    }
                    if (it.osaamisenKartuttaminenYksityinen == true) {
                        it.osaamisenKartuttaminen = null
                    }
                    if (it.elamankenttaYksityinen == true) {
                        it.elamankentta = null
                    }
                }
                ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    private fun getMappedFile(
        file: MultipartFile?,
        opintooikeusId: Long
    ): AsiakirjaDTO? {
        file?.let {
            if (!fileValidationService.validate(
                    listOf(it),
                    opintooikeusId,
                    listOf(MediaType.APPLICATION_PDF_VALUE)
                )
            ) {
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
