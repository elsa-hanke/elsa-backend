package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "kouluttajavaltuutus"

@RestController
@RequestMapping("/api")
class KouluttajavaltuutusResource(
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/kouluttajavaltuutukset")
    fun createKouluttajavaltuutus(
        @Valid @RequestBody kouluttajavaltuutusDTO: KouluttajavaltuutusDTO
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        if (kouluttajavaltuutusDTO.id != null) {
            throw BadRequestAlertException(
                "A new kouluttajavaltuutus cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = kouluttajavaltuutusService.save(kouluttajavaltuutusDTO)
        return ResponseEntity.created(URI("/api/kouluttajavaltuutuses/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/kouluttajavaltuutukset")
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
                    applicationName, true, ENTITY_NAME,
                    kouluttajavaltuutusDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/kouluttajavaltuutukset")
    fun getAllKouluttajavaltuutukset(): MutableList<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusService.findAll()
    }

    @GetMapping("/kouluttajavaltuutukset/{id}")
    fun getKouluttajavaltuutus(
        @PathVariable id: Long
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        val kouluttajavaltuutusDTO = kouluttajavaltuutusService.findOne(id)
        return ResponseUtil.wrapOrNotFound(kouluttajavaltuutusDTO)
    }

    @DeleteMapping("/kouluttajavaltuutukset/{id}")
    fun deleteKouluttajavaltuutus(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        kouluttajavaltuutusService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(
                applicationName,
                true,
                ENTITY_NAME,
                id.toString())
            ).build()
    }
}
