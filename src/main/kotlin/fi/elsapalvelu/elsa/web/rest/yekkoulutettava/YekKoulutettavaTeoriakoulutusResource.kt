package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.OpintoopasService
import fi.elsapalvelu.elsa.service.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutuksetDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/yek-koulutettava")
class YekKoulutettavaTeoriakoulutusResource(
    private val teoriakoulutusService: TeoriakoulutusService,
    private val userService: UserService,
    private val opintoopasService: OpintoopasService,
    private val opintooikeusService: OpintooikeusService
) {

    @GetMapping("/teoriakoulutukset")
    fun getAllTeoriakoulutukset(
        principal: Principal?
    ): ResponseEntity<TeoriakoulutuksetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeus =
            opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(user.id!!, YEK_ERIKOISALA_ID)
        val teoriakoulutukset = teoriakoulutusService.findAll(opintooikeus.id!!)
        val opintoopas = opintoopasService.findOne(opintooikeus.opintoopasId!!)
        return ResponseEntity.ok(
            TeoriakoulutuksetDTO(
                teoriakoulutukset = teoriakoulutukset.toMutableSet(),
                erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
                    ?: 0.0
            )
        )
    }

}
