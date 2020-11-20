package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.service.ArvioitavaOsaalueService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

private const val ENTITY_NAME = "arvioitavaOsaalue"

@RestController
@RequestMapping("/api")
class ArvioitavaOsaalueResource(
    private val arvioitavaOsaalueService: ArvioitavaOsaalueService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/arvioitavat-osaalueet")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createArvioitavaOsaalue(
        @Valid @RequestBody arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO
    ): ResponseEntity<ArvioitavaOsaalueDTO> {
        if (arvioitavaOsaalueDTO.id != null) {
            throw BadRequestAlertException(
                "A new arvioitavaOsaalue cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = arvioitavaOsaalueService.save(arvioitavaOsaalueDTO)
        return ResponseEntity.created(URI("/api/arvioitavat-osaalueet/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/arvioitavat-osaalueet")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun updateArvioitavaOsaalue(
        @Valid @RequestBody arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO
    ): ResponseEntity<ArvioitavaOsaalueDTO> {
        if (arvioitavaOsaalueDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = arvioitavaOsaalueService.save(arvioitavaOsaalueDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    arvioitavaOsaalueDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/arvioitavat-osaalueet")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllArvioitavatOsaalueet(): MutableList<ArvioitavaOsaalueDTO> {
        return arvioitavaOsaalueService.findAll()
    }

    @GetMapping("/arvioitavat-osaalueet/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getArvioitavaOsaalue(
        @PathVariable id: Long
    ): ResponseEntity<ArvioitavaOsaalueDTO> {
        val arvioitavaOsaalueDTO = arvioitavaOsaalueService.findOne(id)
        return ResponseUtil.wrapOrNotFound(arvioitavaOsaalueDTO)
    }

    @DeleteMapping("/arvioitavat-osaalueet/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun deleteArvioitavaOsaalue(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        arvioitavaOsaalueService.delete(id)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString()
                )
            ).build()
    }
}
