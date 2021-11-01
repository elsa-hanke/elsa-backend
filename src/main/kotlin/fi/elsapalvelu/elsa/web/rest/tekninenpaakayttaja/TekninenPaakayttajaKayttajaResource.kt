package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaKayttajaResource(
    private val userService: UserService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    @GetMapping("/erikoistuvat-laakarit")
    fun getErikoistuvatLaakarit(principal: Principal?): ResponseEntity<List<ErikoistuvaLaakariDTO>> {
        return ResponseEntity.ok(erikoistuvaLaakariService.findAll())
    }
}
