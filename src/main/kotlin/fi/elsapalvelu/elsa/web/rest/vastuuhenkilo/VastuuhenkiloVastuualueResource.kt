package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloVastuualueResource(
        private val userService: UserService,
        private val kayttajaService: KayttajaService
) {

    @GetMapping("/onko-terveyskeskuskoulutusjakso-vastuuhenkilo")
    fun getOnkoTerveyskeskuskoulutusjaksoVastuuhenkilo(principal: Principal?): ResponseEntity<Boolean> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        return ResponseEntity.ok(kayttaja.get().yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                    .contains(VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN)
        })
    }

    @GetMapping("/onko-yek-vastuuhenkilo")
    fun getOnkoYekVastuuhenkilo(principal: Principal?): ResponseEntity<Boolean> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        return ResponseEntity.ok(kayttaja.get().yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.YEK_KOULUTUS)
        })
    }
}
