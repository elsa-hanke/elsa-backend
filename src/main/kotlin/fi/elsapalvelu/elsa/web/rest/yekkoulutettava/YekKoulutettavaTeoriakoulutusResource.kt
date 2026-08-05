package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/yek-koulutettava")
class YekKoulutettavaTeoriakoulutusResource(
    private val opintosuoritusService: OpintosuoritusService,
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService
) {

    @GetMapping("/teoriakoulutukset")
    fun getAllTeoriakoulutukset(
        principal: Principal?
    ): ResponseEntity<List<OpintosuoritusDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeus =
            opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(user.id!!, YEK_ERIKOISALA_ID)

        val opintosuorituksetDTO = opintosuoritusService.getOpintosuorituksetByOpintooikeusIdAndTyyppi(
            opintooikeus.id!!, OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS
        )
        return ResponseEntity.ok(opintosuorituksetDTO.opintosuoritukset)
    }


}
