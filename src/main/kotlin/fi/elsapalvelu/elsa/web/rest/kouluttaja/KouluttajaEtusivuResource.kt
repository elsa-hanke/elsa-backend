package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/kouluttaja/etusivu")
class KouluttajaEtusivuResource(
    private val userService: UserService,
    private val etusivuService: EtusivuService
) {

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        principal: Principal?
    ): ResponseEntity<ErikoistujienSeurantaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaForKouluttaja(user.id!!))
    }
}
