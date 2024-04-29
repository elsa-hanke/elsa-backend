package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaMuutToiminnotResource(
    private val erikoisalaService: ErikoisalaService
) {
    @GetMapping("/erikoisalat")
    fun getErikoisalat(): ResponseEntity<List<ErikoisalaDTO>> {
        return ResponseEntity.ok(erikoisalaService.findAllByLiittynytElsaan().filter { it.id != YEK_ERIKOISALA_ID })
    }
}
