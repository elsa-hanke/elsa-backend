package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

private const val ENTITY_NAME = "kouluttajavaltuutus"

@RestController
@RequestMapping("/api")
class KouluttajavaltuutusResource(
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/kouluttajavaltuutukset")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createKouluttajavaltuutus(
        @Valid @RequestBody kouluttajavaltuutusDTO: KouluttajavaltuutusDTO
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        if (kouluttajavaltuutusDTO.id != null) {
            throw BadRequestAlertException(
                "A new kouluttajavaltuutus cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = kouluttajavaltuutusService.save(kouluttajavaltuutusDTO)
        return ResponseEntity.created(URI("/api/kouluttajavaltuutuses/${result.id}"))
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

    @PutMapping("/kouluttajavaltuutukset")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun updateKouluttajavaltuutus(
        @Valid @RequestBody kouluttajavaltuutusDTO: KouluttajavaltuutusDTO
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        if (kouluttajavaltuutusDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = kouluttajavaltuutusService.save(kouluttajavaltuutusDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    kouluttajavaltuutusDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/kouluttajavaltuutukset")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllKouluttajavaltuutukset(): MutableList<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusService.findAll()
    }

    @GetMapping("/kouluttajavaltuutukset/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getKouluttajavaltuutus(
        @PathVariable id: Long
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        val kouluttajavaltuutusDTO = kouluttajavaltuutusService.findOne(id)
        return ResponseUtil.wrapOrNotFound(kouluttajavaltuutusDTO)
    }

    @DeleteMapping("/kouluttajavaltuutukset/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun deleteKouluttajavaltuutus(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        kouluttajavaltuutusService.delete(id)
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
