package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

private const val ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val kayttajaService: KayttajaService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/kayttajat")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createKayttaja(
        @Valid @RequestBody kayttajaDTO: KayttajaDTO
    ): ResponseEntity<KayttajaDTO> {
        if (kayttajaDTO.id != null) {
            throw BadRequestAlertException(
                "A new kayttaja cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = kayttajaService.save(kayttajaDTO)
        return ResponseEntity.created(URI("/api/kayttajat/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/kayttajat")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun updateKayttaja(
        @Valid @RequestBody kayttajaDTO: KayttajaDTO
    ): ResponseEntity<KayttajaDTO> {
        if (kayttajaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = kayttajaService.save(kayttajaDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    kayttajaDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/kayttajat")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getAllKayttajat(): MutableList<KayttajaDTO> {
        return kayttajaService.findAll()
    }

    @GetMapping("/kayttajat/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun getKayttaja(
        @PathVariable id: Long
    ): ResponseEntity<KayttajaDTO> {
        val kayttajaDTO = kayttajaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(kayttajaDTO)
    }

    @DeleteMapping("/kayttajat/{id}")
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun deleteKayttaja(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        kayttajaService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(
                applicationName,
                true,
                ENTITY_NAME,
                id.toString())
            ).build()
    }
}
