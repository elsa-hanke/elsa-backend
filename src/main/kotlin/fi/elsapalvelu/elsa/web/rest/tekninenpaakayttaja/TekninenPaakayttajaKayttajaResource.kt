package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaKayttajaResource(
    private val userService: UserService,
) {

    @GetMapping("/kayttajat")
    fun getKayttajat(principal: Principal?): ResponseEntity<List<KayttajaDTO>> {
        return ResponseEntity.ok(emptyList())
    }
}
