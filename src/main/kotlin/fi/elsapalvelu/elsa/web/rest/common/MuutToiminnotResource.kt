package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
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
    private val arviointityokaluService: ArviointityokaluService
) {

    @GetMapping("/kouluttajat")
    fun getKouluttajat(
        principal: Principal?
    ): ResponseEntity<List<KayttajaDTO>> {
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
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val arviointityokaluDTO = arviointityokaluService.findAllByKayttajaId(kayttaja.id!!)
        return ResponseEntity.ok(arviointityokaluDTO)
    }


}
