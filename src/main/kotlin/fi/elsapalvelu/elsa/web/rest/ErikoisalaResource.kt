package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException
import javax.validation.Valid

private const val ENTITY_NAME = "erikoisala"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.Erikoisala].
 */
@RestController
@RequestMapping("/api")
class ErikoisalaResource(
    private val erikoisalaService: ErikoisalaService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /erikoisalas` : Create a new erikoisala.
     *
     * @param erikoisalaDTO the erikoisalaDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new erikoisalaDTO,
     * or with status `400 (Bad Request)` if the erikoisala has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/erikoisalas")
    fun createErikoisala(@Valid @RequestBody erikoisalaDTO: ErikoisalaDTO): ResponseEntity<ErikoisalaDTO> {
        log.debug("REST request to save Erikoisala : $erikoisalaDTO")
        if (erikoisalaDTO.id != null) {
            throw BadRequestAlertException(
                "A new erikoisala cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = erikoisalaService.save(erikoisalaDTO)
        return ResponseEntity.created(URI("/api/erikoisalas/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /erikoisalas` : Updates an existing erikoisala.
     *
     * @param erikoisalaDTO the erikoisalaDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated erikoisalaDTO,
     * or with status `400 (Bad Request)` if the erikoisalaDTO is not valid,
     * or with status `500 (Internal Server Error)` if the erikoisalaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/erikoisalas")
    fun updateErikoisala(@Valid @RequestBody erikoisalaDTO: ErikoisalaDTO): ResponseEntity<ErikoisalaDTO> {
        log.debug("REST request to update Erikoisala : $erikoisalaDTO")
        if (erikoisalaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = erikoisalaService.save(erikoisalaDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    erikoisalaDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /erikoisalas` : get all the erikoisalas.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of erikoisalas in body.
     */
    @GetMapping("/erikoisalas")
    fun getAllErikoisalas(): MutableList<ErikoisalaDTO> {
        log.debug("REST request to get all Erikoisalas")

        return erikoisalaService.findAll()
    }

    /**
     * `GET  /erikoisalas/:id` : get the "id" erikoisala.
     *
     * @param id the id of the erikoisalaDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the erikoisalaDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/erikoisalas/{id}")
    fun getErikoisala(@PathVariable id: Long): ResponseEntity<ErikoisalaDTO> {
        log.debug("REST request to get Erikoisala : $id")
        val erikoisalaDTO = erikoisalaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(erikoisalaDTO)
    }
    /**
     *  `DELETE  /erikoisalas/:id` : delete the "id" erikoisala.
     *
     * @param id the id of the erikoisalaDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/erikoisalas/{id}")
    fun deleteErikoisala(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Erikoisala : $id")

        erikoisalaService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
