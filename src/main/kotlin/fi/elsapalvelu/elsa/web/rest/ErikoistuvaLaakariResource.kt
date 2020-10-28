package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

private const val ENTITY_NAME = "erikoistuvaLaakari"

@RestController
@RequestMapping("/api")
class ErikoistuvaLaakariResource(
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/erikoistuvat-laakarit")
    fun createErikoistuvaLaakari(
        @Valid @RequestBody erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        if (erikoistuvaLaakariDTO.id != null) {
            throw BadRequestAlertException(
                "A new erikoistuvaLaakari cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = erikoistuvaLaakariService.save(erikoistuvaLaakariDTO)
        return ResponseEntity.created(URI("/api/erikoistuvat-laakarit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/erikoistuvat-laakarit")
    fun updateErikoistuvaLaakari(
        @Valid @RequestBody erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        if (erikoistuvaLaakariDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = erikoistuvaLaakariService.save(erikoistuvaLaakariDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    erikoistuvaLaakariDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/erikoistuvat-laakarit")
    fun getAllErikoistuvaLaakarit(
        @RequestParam(required = false) filter: String?
    ): MutableList<ErikoistuvaLaakariDTO> {
        return erikoistuvaLaakariService.findAll()
    }

    @GetMapping("/erikoistuvat-laakarit/{id}")
    fun getErikoistuvaLaakari(
        @PathVariable id: Long
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val erikoistuvaLaakariDTO = erikoistuvaLaakariService.findOne(id)
        return ResponseUtil.wrapOrNotFound(erikoistuvaLaakariDTO)
    }

    @DeleteMapping("/erikoistuvat-laakarit/{id}")
    fun deleteErikoistuvaLaakari(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        erikoistuvaLaakariService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(
                applicationName,
                true,
                ENTITY_NAME,
                id.toString())
            ).build()
    }
}
