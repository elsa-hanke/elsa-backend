package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.OpintosuoritusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariOpintosuoritusResource(
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val opintosuoritusService: OpintosuoritusService
) {

    @GetMapping("/opintosuoritukset")
    fun getOpintosuoritukset(principal: Principal?): ResponseEntity<OpintosuorituksetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val opintosuoritukset = opintosuoritusService.getOpintosuorituksetByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(opintosuoritukset)
    }
}
