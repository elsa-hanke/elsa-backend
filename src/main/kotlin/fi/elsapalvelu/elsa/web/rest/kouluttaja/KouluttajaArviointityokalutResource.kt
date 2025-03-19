package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaArviointityokalutResource (
    private val arviointityokaluService: ArviointityokaluService,
) {
    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(): ResponseEntity<List<ArviointityokaluDTO>> {
        return ResponseEntity.ok(arviointityokaluService.findAllJulkaistut())
    }
}
