package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluService
import fi.elsapalvelu.elsa.service.perustiedot.YliopistoService
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.HakaYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.YliopistoDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/")
class MuutToiminnotResource(
    private val yliopistoService: YliopistoService,
    private val arviointityokaluService: ArviointityokaluService
) {
    @GetMapping("/yliopistot")
    fun getYliopistot(): ResponseEntity<List<YliopistoDTO>> {
        return ResponseEntity.ok(yliopistoService.findAll())
    }

    @GetMapping("/haka-yliopistot")
    fun getHakaYliopistot(): ResponseEntity<List<HakaYliopistoDTO>> {
        return ResponseEntity.ok(yliopistoService.findAllHaka())
    }

    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(): ResponseEntity<List<ArviointityokaluDTO>> {
        val arviointityokaluDTO = arviointityokaluService.findAllJulkaistu()
        return ResponseEntity.ok(arviointityokaluDTO)
    }


}
