package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.dto.LaillistamispaivaDTO
import fi.elsapalvelu.elsa.service.impl.UserServiceImpl
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.time.LocalDate

private const val ERIKOISTUVA_LAAKARI_ENTITY_NAME = "erikoistuvaLaakari"

@RestController
@RequestMapping("/api/yek-koulutettava")
class YekKoulutettavaMuutToiminnotResource(
    private val userService: UserServiceImpl,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val opintooikeusService: OpintooikeusService,
) {

    @PutMapping("/laillistamispaiva")
    fun updateYekKoulutettavaLaillistamispaiva(
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        @RequestParam(required = false) laakarikoulutusSuoritettuSuomiTaiBelgia: Boolean?,
        @RequestParam(required = false) laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: Boolean?,
        principal: Principal?
    ) {
        val user = userService.getAuthenticatedUser(principal)

        validateMuokkausoikeudet(principal, user.id!!, ERIKOISTUVA_LAAKARI_ENTITY_NAME)

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        erikoistuvaLaakariService.updateLaakarikoulutusSuoritettuSuomiTaiBelgia(
            user.id!!, laakarikoulutusSuoritettuSuomiTaiBelgia, laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
        )
    }

    @GetMapping("/laillistamispaiva")
    fun getLaillistamispaiva(
        principal: Principal?
    ): ResponseEntity<LaillistamispaivaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.getLaillistamispaiva(user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    private fun validateMuokkausoikeudet(principal: Principal?, userId: String, entity: String) {
        if ((principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
                .contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
        ) {
            val opintooikeus =
                opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)
            if (!opintooikeus.muokkausoikeudetVirkailijoilla) {
                throw BadRequestAlertException(
                    "Ei oikeuksia muokata yek koulutettavan tietoja",
                    entity,
                    "dataillegal.ei-oikeuksia-muokata-erikoistujan-tietoja"
                )
            }
        }
    }

}
