package fi.elsapalvelu.elsa.web.rest.muu

import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/")
class MuutToiminnotResource(
    private val kayttajaService: KayttajaService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/kouluttajat")
    fun getKouluttajat(
        principal: Principal?
    ): ResponseEntity<List<KayttajaDTO>> {
        log.debug("REST request to get Kouluttajat")
        return ResponseEntity.ok(kayttajaService.findKouluttajat())
    }
}
