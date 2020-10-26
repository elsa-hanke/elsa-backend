package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

private const val ENTITY_NAME = "tyoskentelyjakso"

@RestController
@RequestMapping("/api")
class TyoskentelyjaksoResource(
    private val tyoskentelyjaksoService: TyoskentelyjaksoService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "A new tyoskentelyjakso cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = tyoskentelyjaksoService.save(tyoskentelyjaksoDTO)
        return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = tyoskentelyjaksoService.save(tyoskentelyjaksoDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    tyoskentelyjaksoDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getAlltyoskentelyjaksot(@RequestParam(required = false) filter: String?): MutableList<TyoskentelyjaksoDTO> {
        if ("tyoskentelypaikka-is-null".equals(filter)) {
            return tyoskentelyjaksoService.findAllWhereTyoskentelypaikkaIsNull()
        }

        return tyoskentelyjaksoService.findAll()
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(@PathVariable id: Long): ResponseEntity<TyoskentelyjaksoDTO> {
        val tyoskentelyjaksoDTO = tyoskentelyjaksoService.findOne(id)
        return ResponseUtil.wrapOrNotFound(tyoskentelyjaksoDTO)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(@PathVariable id: Long): ResponseEntity<Void> {
        tyoskentelyjaksoService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
