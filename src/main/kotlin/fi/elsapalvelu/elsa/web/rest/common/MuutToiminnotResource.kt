package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.HakaYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/")
class MuutToiminnotResource(
    private val yliopistoService: YliopistoService,
    private val userService: UserService,
    private val arviointityokaluService: ArviointityokaluService
) {
    @GetMapping("/yliopistot")
    fun getYliopistot(
        principal: Principal?
    ): ResponseEntity<List<YliopistoDTO>> {
        return ResponseEntity.ok(yliopistoService.findAll())
    }

    @GetMapping("/haka-yliopistot")
    fun getHakaYliopistot(
        principal: Principal?
    ): ResponseEntity<List<HakaYliopistoDTO>> {
        return ResponseEntity.ok(yliopistoService.findAllHaka())
    }

    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(
        principal: Principal?
    ): ResponseEntity<List<ArviointityokaluDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        // val arviointityokaluDTO = arviointityokaluService.findAllByKayttajaUserId(user.id!!)
        val arviointityokaluDTO = arviointityokaluService.findAllJulkaistu()
        return ResponseEntity.ok(arviointityokaluDTO)
    }


}
