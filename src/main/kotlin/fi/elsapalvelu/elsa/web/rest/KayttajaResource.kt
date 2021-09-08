package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val kayttajaService: KayttajaService
) {

    @GetMapping("/kayttaja")
    fun getKayttaja(principal: Principal?): KayttajaDTO {
        return kayttajaService.getAuthenticatedKayttaja(principal)
    }
}
