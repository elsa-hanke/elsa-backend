package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.service.SoteOrganisaatioQueryService
import fi.elsapalvelu.elsa.service.SoteOrganisaatioService
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioCriteria
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI

private const val ENTITY_NAME = "soteOrganisaatio"

@RestController
@RequestMapping("/api")
class SoteOrganisaatioResource(
    private val soteOrganisaatioService: SoteOrganisaatioService,
    private val soteOrganisaatioQueryService: SoteOrganisaatioQueryService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/sote-organisaatiot")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createSoteOrganisaatio(
        @RequestBody soteOrganisaatioDTO: SoteOrganisaatioDTO
    ): ResponseEntity<SoteOrganisaatioDTO> {
        if (soteOrganisaatioDTO.organizationId != null) {
            throw BadRequestAlertException(
                "A new soteOrganisaatio cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = soteOrganisaatioService.save(soteOrganisaatioDTO)
        return ResponseEntity.created(URI("/api/sote-organisaatiot/${result.organizationId}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    result.organizationId.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/sote-organisaatiot")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun updateSoteOrganisaatio(
        @RequestBody soteOrganisaatioDTO: SoteOrganisaatioDTO
    ): ResponseEntity<SoteOrganisaatioDTO> {
        if (soteOrganisaatioDTO.organizationId == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = soteOrganisaatioService.save(soteOrganisaatioDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    soteOrganisaatioDTO.organizationId.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/sote-organisaatiot")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllSoteOrganisaatiot(
        criteria: SoteOrganisaatioCriteria
    ): ResponseEntity<MutableList<SoteOrganisaatioDTO>> {
        val entityList = soteOrganisaatioQueryService.findByCriteria(criteria)
        return ResponseEntity.ok().body(entityList)
    }

    @GetMapping("/sote-organisaatiot/kunnat")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllKunnat(): ResponseEntity<MutableList<String>> {
        return ResponseEntity.ok(soteOrganisaatioQueryService.findAllKunnat())
    }

    @GetMapping("/sote-organisaatiot/count")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun countSoteOrganisaatiot(
        criteria: SoteOrganisaatioCriteria
    ): ResponseEntity<Long> {
        return ResponseEntity.ok().body(soteOrganisaatioQueryService.countByCriteria(criteria))
    }

    @GetMapping("/sote-organisaatiot/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getSoteOrganisaatio(
        @PathVariable id: String
    ): ResponseEntity<SoteOrganisaatioDTO> {
        val soteOrganisaatioDTO = soteOrganisaatioService.findOne(id)
        return ResponseUtil.wrapOrNotFound(soteOrganisaatioDTO)
    }

    @DeleteMapping("/sote-organisaatiot/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun deleteSoteOrganisaatio(
        @PathVariable id: String
    ): ResponseEntity<Void> {
        soteOrganisaatioService.delete(id)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id
                )
            ).build()
    }
}
