package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.TyoskentelyjaksoService
import fi.oulu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException
import javax.validation.Valid

private const val ENTITY_NAME = "tyoskentelyjakso"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Tyoskentelyjakso].
 */
@RestController
@RequestMapping("/api")
class TyoskentelyjaksoResource(
    private val tyoskentelyjaksoService: TyoskentelyjaksoService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /tyoskentelyjaksos` : Create a new tyoskentelyjakso.
     *
     * @param tyoskentelyjaksoDTO the tyoskentelyjaksoDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new tyoskentelyjaksoDTO,
     * or with status `400 (Bad Request)` if the tyoskentelyjakso has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/tyoskentelyjaksos")
    fun createTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to save Tyoskentelyjakso : $tyoskentelyjaksoDTO")
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "A new tyoskentelyjakso cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = tyoskentelyjaksoService.save(tyoskentelyjaksoDTO)
        return ResponseEntity.created(URI("/api/tyoskentelyjaksos/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /tyoskentelyjaksos` : Updates an existing tyoskentelyjakso.
     *
     * @param tyoskentelyjaksoDTO the tyoskentelyjaksoDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated tyoskentelyjaksoDTO,
     * or with status `400 (Bad Request)` if the tyoskentelyjaksoDTO is not valid,
     * or with status `500 (Internal Server Error)` if the tyoskentelyjaksoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/tyoskentelyjaksos")
    fun updateTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to update Tyoskentelyjakso : $tyoskentelyjaksoDTO")
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
    /**
     * `GET  /tyoskentelyjaksos` : get all the tyoskentelyjaksos.
     *

     * @param filter the filter of the request.
     * @return the [ResponseEntity] with status `200 (OK)` and the list of tyoskentelyjaksos in body.
     */
    @GetMapping("/tyoskentelyjaksos")
    fun getAllTyoskentelyjaksos(@RequestParam(required = false) filter: String?): MutableList<TyoskentelyjaksoDTO> {
        if ("tyoskentelypaikka-is-null".equals(filter)) {
            log.debug("REST request to get all Tyoskentelyjaksos where tyoskentelypaikka is null")
            return tyoskentelyjaksoService.findAllWhereTyoskentelypaikkaIsNull()
        }
        if ("arviointi-is-null".equals(filter)) {
            log.debug("REST request to get all Tyoskentelyjaksos where arviointi is null")
            return tyoskentelyjaksoService.findAllWhereArviointiIsNull()
        }
        log.debug("REST request to get all Tyoskentelyjaksos")

        return tyoskentelyjaksoService.findAll()
    }

    /**
     * `GET  /tyoskentelyjaksos/:id` : get the "id" tyoskentelyjakso.
     *
     * @param id the id of the tyoskentelyjaksoDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the tyoskentelyjaksoDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/tyoskentelyjaksos/{id}")
    fun getTyoskentelyjakso(@PathVariable id: Long): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to get Tyoskentelyjakso : $id")
        val tyoskentelyjaksoDTO = tyoskentelyjaksoService.findOne(id)
        return ResponseUtil.wrapOrNotFound(tyoskentelyjaksoDTO)
    }
    /**
     *  `DELETE  /tyoskentelyjaksos/:id` : delete the "id" tyoskentelyjakso.
     *
     * @param id the id of the tyoskentelyjaksoDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/tyoskentelyjaksos/{id}")
    fun deleteTyoskentelyjakso(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
