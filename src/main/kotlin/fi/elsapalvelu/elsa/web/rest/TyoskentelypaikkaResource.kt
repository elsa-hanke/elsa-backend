package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.TyoskentelypaikkaService
import fi.elsapalvelu.elsa.service.dto.TyoskentelypaikkaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "tyoskentelypaikka"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.Tyoskentelypaikka].
 */
@RestController
@RequestMapping("/api")
class TyoskentelypaikkaResource(
    private val tyoskentelypaikkaService: TyoskentelypaikkaService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /tyoskentelypaikkas` : Create a new tyoskentelypaikka.
     *
     * @param tyoskentelypaikkaDTO the tyoskentelypaikkaDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new tyoskentelypaikkaDTO,
     * or with status `400 (Bad Request)` if the tyoskentelypaikka has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/tyoskentelypaikkas")
    fun createTyoskentelypaikka(
        @RequestBody tyoskentelypaikkaDTO: TyoskentelypaikkaDTO
    ): ResponseEntity<TyoskentelypaikkaDTO> {
        log.debug("REST request to save Tyoskentelypaikka : $tyoskentelypaikkaDTO")
        if (tyoskentelypaikkaDTO.id != null) {
            throw BadRequestAlertException(
                "A new tyoskentelypaikka cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = tyoskentelypaikkaService.save(tyoskentelypaikkaDTO)
        return ResponseEntity.created(URI("/api/tyoskentelypaikkas/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /tyoskentelypaikkas` : Updates an existing tyoskentelypaikka.
     *
     * @param tyoskentelypaikkaDTO the tyoskentelypaikkaDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated tyoskentelypaikkaDTO,
     * or with status `400 (Bad Request)` if the tyoskentelypaikkaDTO is not valid,
     * or with status `500 (Internal Server Error)` if the tyoskentelypaikkaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/tyoskentelypaikkas")
    fun updateTyoskentelypaikka(
        @RequestBody tyoskentelypaikkaDTO: TyoskentelypaikkaDTO
    ): ResponseEntity<TyoskentelypaikkaDTO> {
        log.debug("REST request to update Tyoskentelypaikka : $tyoskentelypaikkaDTO")
        if (tyoskentelypaikkaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = tyoskentelypaikkaService.save(tyoskentelypaikkaDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    tyoskentelypaikkaDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /tyoskentelypaikkas` : get all the tyoskentelypaikkas.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of tyoskentelypaikkas in body.
     */
    @GetMapping("/tyoskentelypaikkas")
    fun getAllTyoskentelypaikkas(): MutableList<TyoskentelypaikkaDTO> {
        log.debug("REST request to get all Tyoskentelypaikkas")

        return tyoskentelypaikkaService.findAll()
    }

    /**
     * `GET  /tyoskentelypaikkas/:id` : get the "id" tyoskentelypaikka.
     *
     * @param id the id of the tyoskentelypaikkaDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the tyoskentelypaikkaDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/tyoskentelypaikkas/{id}")
    fun getTyoskentelypaikka(@PathVariable id: Long): ResponseEntity<TyoskentelypaikkaDTO> {
        log.debug("REST request to get Tyoskentelypaikka : $id")
        val tyoskentelypaikkaDTO = tyoskentelypaikkaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(tyoskentelypaikkaDTO)
    }
    /**
     *  `DELETE  /tyoskentelypaikkas/:id` : delete the "id" tyoskentelypaikka.
     *
     * @param id the id of the tyoskentelypaikkaDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/tyoskentelypaikkas/{id}")
    fun deleteTyoskentelypaikka(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Tyoskentelypaikka : $id")

        tyoskentelypaikkaService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
