package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.SoteOrganisaatioQueryService
import fi.elsapalvelu.elsa.service.SoteOrganisaatioService
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioCriteria
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    fun createSoteOrganisaatio(
        @RequestBody soteOrganisaatioDTO: SoteOrganisaatioDTO
    ): ResponseEntity<SoteOrganisaatioDTO> {
        if (soteOrganisaatioDTO.organizationId != null) {
            throw BadRequestAlertException(
                "A new soteOrganisaatio cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = soteOrganisaatioService.save(soteOrganisaatioDTO)
        return ResponseEntity.created(URI("/api/sote-organisaatiot/${result.organizationId}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.organizationId.toString())
            )
            .body(result)
    }

    @PutMapping("/sote-organisaatiot")
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
                    applicationName, true, ENTITY_NAME,
                    soteOrganisaatioDTO.organizationId.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/sote-organisaatiot") fun getAllSoteOrganisaatiot(
        criteria: SoteOrganisaatioCriteria
    ): ResponseEntity<MutableList<SoteOrganisaatioDTO>> {
        val entityList = soteOrganisaatioQueryService.findByCriteria(criteria)
        return ResponseEntity.ok().body(entityList)
    }

    @GetMapping("/sote-organisaatiot/kunnat") fun getAllKunnat(
    ): ResponseEntity<MutableList<String>> {
        return ResponseEntity.ok(soteOrganisaatioQueryService.findAllKunnat())
    }

    @GetMapping("/sote-organisaatiot/count")
    fun countSoteOrganisaatiot(
        criteria: SoteOrganisaatioCriteria
    ): ResponseEntity<Long> {
        return ResponseEntity.ok().body(soteOrganisaatioQueryService.countByCriteria(criteria))
    }

    @GetMapping("/sote-organisaatiot/{id}")
    fun getSoteOrganisaatio(
        @PathVariable id: String
    ): ResponseEntity<SoteOrganisaatioDTO> {
        val soteOrganisaatioDTO = soteOrganisaatioService.findOne(id)
        return ResponseUtil.wrapOrNotFound(soteOrganisaatioDTO)
    }

    @DeleteMapping("/sote-organisaatiot/{id}")
    fun deleteSoteOrganisaatio(
        @PathVariable id: String
    ): ResponseEntity<Void> {
        soteOrganisaatioService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(
                applicationName,
                true,
                ENTITY_NAME,
                id
            )).build()
    }
}
