package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.VastuuhenkilonVastuualueetDTO
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

    @GetMapping("/vastuualueet")
    fun getVastuualueet(principal: Principal?): ResponseEntity<VastuuhenkilonVastuualueetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!).get()
        val tkJakso = kayttaja.yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN)
        }
        val yekTkJakso = kayttaja.yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO)
        }
        val valmistuminen = kayttaja.yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA) ||
                it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                    .contains(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI)
        }
        val yekValmistuminen = kayttaja.yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN)
        }
        return ResponseEntity.ok(VastuuhenkilonVastuualueetDTO(
            terveyskeskuskoulutusjakso = tkJakso ?: false,
            yekTerveyskeskuskoulutusjakso = yekTkJakso ?: false,
            valmistuminen = valmistuminen ?: false,
            yekValmistuminen = yekValmistuminen ?: false))
    }
}
