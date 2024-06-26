package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AvoinAsiaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistumisenEdistyminenDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/yek-koulutettava/etusivu")
class YekKoulutettavaEtusivuResource(
    private val userService: UserService,
    private val etusivuService: EtusivuService
) {

    @GetMapping("/avoimet-asiat")
    fun getAvoimetAsiat(
        principal: Principal?
    ): ResponseEntity<List<AvoinAsiaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getAvoimetAsiatForYekKoulutettava(user.id!!))
    }

    @GetMapping("/erikoistumisen-edistyminen")
    fun getErikoistumisenEdistyminen(
        principal: Principal?
    ): ResponseEntity<ErikoistumisenEdistyminenDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistumisenSeurantaForErikoistuja(user.id!!))
    }
}
