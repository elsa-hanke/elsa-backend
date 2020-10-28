package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.OsaalueenArviointiService
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "osaalueenArviointi"

@RestController
@RequestMapping("/api")
class OsaalueenArviointiResource(
    private val osaalueenArviointiService: OsaalueenArviointiService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/osaalueen-arviointis")
    fun createOsaalueenArviointi(
        @Valid @RequestBody osaalueenArviointiDTO: OsaalueenArviointiDTO
    ): ResponseEntity<OsaalueenArviointiDTO> {
        if (osaalueenArviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new osaalueenArviointi cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = osaalueenArviointiService.save(osaalueenArviointiDTO)
        return ResponseEntity.created(URI("/api/osaalueen-arviointis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/osaalueen-arviointis")
    fun updateOsaalueenArviointi(
        @Valid @RequestBody osaalueenArviointiDTO: OsaalueenArviointiDTO
    ): ResponseEntity<OsaalueenArviointiDTO> {
        if (osaalueenArviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = osaalueenArviointiService.save(osaalueenArviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     osaalueenArviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/osaalueen-arviointis")
    fun getAllOsaalueenArviointis(): MutableList<OsaalueenArviointiDTO> {

        return osaalueenArviointiService.findAll()
    }

    @GetMapping("/osaalueen-arviointis/{id}")
    fun getOsaalueenArviointi(
        @PathVariable id: Long
    ): ResponseEntity<OsaalueenArviointiDTO> {
        val osaalueenArviointiDTO = osaalueenArviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(osaalueenArviointiDTO)
    }

    @DeleteMapping("/osaalueen-arviointis/{id}")
    fun deleteOsaalueenArviointi(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        osaalueenArviointiService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString())
                ).build()
    }
}
