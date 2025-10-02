package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.ArviointityokaluKategoriaService
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.security.Principal

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaArviointityokalutResource (
    private val arviointityokaluService: ArviointityokaluService,
    private val arviointityokaluKategoriaService: ArviointityokaluKategoriaService,
) {
    @GetMapping("/arviointityokalut")
    fun getArviointityokalut(): ResponseEntity<List<ArviointityokaluDTO>> {
        return ResponseEntity.ok(arviointityokaluService.findAllJulkaistut())
    }

    @GetMapping("/arviointityokalut/kategoriat")
    fun getArviointityokaluKategoriat(): ResponseEntity<List<ArviointityokaluKategoriaDTO>> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.findAll())
    }

    @GetMapping("/asiakirjat/{id}")
    fun getAsiakirja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val arviointityokalu = arviointityokaluService.findOneByLiiteId(id)
        val asiakirjaData: AsiakirjaDataDTO = arviointityokaluService.getAsiakirjaDataDTO(arviointityokalu.liite)
        asiakirjaData.fileInputStream?.use {
            return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(arviointityokalu.liitetiedostonNimi, "UTF-8") + "\""
                )
                .header(HttpHeaders.CONTENT_TYPE, arviointityokalu.liitetiedostonTyyppi + "; charset=UTF-8")
                .body(it.readBytes())
        }

        return ResponseEntity.notFound().build()
    }

}
