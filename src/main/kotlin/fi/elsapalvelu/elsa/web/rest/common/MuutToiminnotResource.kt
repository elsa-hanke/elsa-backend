package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/")
class MuutToiminnotResource(
    private val kayttajaService: KayttajaService,
    private val yliopistoService: YliopistoService,
    private val userService: UserService,
    private val arviointityokaluService: ArviointityokaluService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/kouluttajat")
    fun getKouluttajat(
        principal: Principal?
    ): ResponseEntity<List<KayttajaDTO>> {
        log.debug("REST request to get Kouluttajat")
        return ResponseEntity.ok(kayttajaService.findKouluttajat())
    }

    @GetMapping("/yliopistot")
    fun getYliopistot(
        principal: Principal?
    ): ResponseEntity<List<YliopistoDTO>> {
        return ResponseEntity.ok(yliopistoService.findAll())
    }

    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(
        principal: Principal?
    ): ResponseEntity<List<ArviointityokaluDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val arviointityokaluDTO = arviointityokaluService.findAllByKayttajaUserId(user.id!!)
        return ResponseEntity.ok(arviointityokaluDTO)
    }


}
