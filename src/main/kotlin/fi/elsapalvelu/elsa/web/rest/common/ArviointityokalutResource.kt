package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluKategoriaService
import fi.elsapalvelu.elsa.service.arviointi.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluKategoriaDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

abstract class ArviointityokalutResource(
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
        @PathVariable id: Long
    ): ResponseEntity<ByteArray> {
        val arviointityokalu = arviointityokaluService.findOneByLiiteId(id)
        val asiakirjaData = arviointityokaluService.getAsiakirjaDataDTO(arviointityokalu.liite)
        return asiakirjaData.fileInputStream
            ?.toFileDownloadResponse(
                arviointityokalu.liitetiedostonNimi ?: "",
                arviointityokalu.liitetiedostonTyyppi ?: ""
            )
            ?: ResponseEntity.notFound().build()
    }
}
