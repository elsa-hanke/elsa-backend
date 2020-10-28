package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "epaOsaamisalue"

@RestController
@RequestMapping("/api")
class EpaOsaamisalueResource(
    private val epaOsaamisalueService: EpaOsaamisalueService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/epa-osaamisalueet")
    fun createEpaOsaamisalue(
        @Valid @RequestBody epaOsaamisalueDTO: EpaOsaamisalueDTO
    ): ResponseEntity<EpaOsaamisalueDTO> {
        if (epaOsaamisalueDTO.id != null) {
            throw BadRequestAlertException(
                "A new epaOsaamisalue cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = epaOsaamisalueService.save(epaOsaamisalueDTO)
        return ResponseEntity.created(URI("/api/epa-osaamisalueet/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/epa-osaamisalueet")
    fun updateEpaOsaamisalue(
        @Valid @RequestBody epaOsaamisalueDTO: EpaOsaamisalueDTO
    ): ResponseEntity<EpaOsaamisalueDTO> {
        if (epaOsaamisalueDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = epaOsaamisalueService.save(epaOsaamisalueDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     epaOsaamisalueDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/epa-osaamisalueet")
    fun getAllEpaOsaamisalueet(): MutableList<EpaOsaamisalueDTO> {

        return epaOsaamisalueService.findAll()
    }

    @GetMapping("/epa-osaamisalueet/{id}")
    fun getEpaOsaamisalue(
        @PathVariable id: Long
    ): ResponseEntity<EpaOsaamisalueDTO> {
        val epaOsaamisalueDTO = epaOsaamisalueService.findOne(id)
        return ResponseUtil.wrapOrNotFound(epaOsaamisalueDTO)
    }

    @DeleteMapping("/epa-osaamisalueet/{id}")
    fun deleteEpaOsaamisalue(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        epaOsaamisalueService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString())
                ).build()
    }
}
