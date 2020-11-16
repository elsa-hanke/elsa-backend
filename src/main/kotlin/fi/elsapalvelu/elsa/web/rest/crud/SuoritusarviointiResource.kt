package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "suoritusarviointi"

@RestController
@RequestMapping("/api")
class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritusarvioinnit")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new suoritusarviointi cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/suoritusarvioinnit")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun updateSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     suoritusarviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/suoritusarvioinnit")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllSuoritusarvioinnit(
        criteria: SuoritusarviointiCriteria
    ): ResponseEntity<MutableList<SuoritusarviointiDTO>> {
        val entityList = suoritusarviointiQueryService.findByCriteria(criteria)
        return ResponseEntity.ok().body(entityList)
    }

    @GetMapping("/suoritusarvioinnit/count")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun countSuoritusarvioinnit(criteria: SuoritusarviointiCriteria): ResponseEntity<Long> {
        return ResponseEntity.ok().body(suoritusarviointiQueryService.countByCriteria(criteria))
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getSuoritusarviointi(
        @PathVariable id: Long
    ): ResponseEntity<SuoritusarviointiDTO> {
        val suoritusarviointiDTO = suoritusarviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @DeleteMapping("/suoritusarvioinnit/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun deleteSuoritusarviointi(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        suoritusarviointiService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString()
                )).build()
    }
}
