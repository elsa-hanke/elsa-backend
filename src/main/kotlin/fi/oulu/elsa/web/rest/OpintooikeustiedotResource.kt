package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.OpintooikeustiedotService
import fi.oulu.elsa.service.dto.OpintooikeustiedotDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "opintooikeustiedot"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Opintooikeustiedot].
 */
@RestController
@RequestMapping("/api")
class OpintooikeustiedotResource(
    private val opintooikeustiedotService: OpintooikeustiedotService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /opintooikeustiedots` : Create a new opintooikeustiedot.
     *
     * @param opintooikeustiedotDTO the opintooikeustiedotDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new opintooikeustiedotDTO, or with status `400 (Bad Request)` if the opintooikeustiedot has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/opintooikeustiedots")
    fun createOpintooikeustiedot(@RequestBody opintooikeustiedotDTO: OpintooikeustiedotDTO): ResponseEntity<OpintooikeustiedotDTO> {
        log.debug("REST request to save Opintooikeustiedot : $opintooikeustiedotDTO")
        if (opintooikeustiedotDTO.id != null) {
            throw BadRequestAlertException(
                "A new opintooikeustiedot cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = opintooikeustiedotService.save(opintooikeustiedotDTO)
        return ResponseEntity.created(URI("/api/opintooikeustiedots/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /opintooikeustiedots` : Updates an existing opintooikeustiedot.
     *
     * @param opintooikeustiedotDTO the opintooikeustiedotDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated opintooikeustiedotDTO,
     * or with status `400 (Bad Request)` if the opintooikeustiedotDTO is not valid,
     * or with status `500 (Internal Server Error)` if the opintooikeustiedotDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/opintooikeustiedots")
    fun updateOpintooikeustiedot(@RequestBody opintooikeustiedotDTO: OpintooikeustiedotDTO): ResponseEntity<OpintooikeustiedotDTO> {
        log.debug("REST request to update Opintooikeustiedot : $opintooikeustiedotDTO")
        if (opintooikeustiedotDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = opintooikeustiedotService.save(opintooikeustiedotDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    opintooikeustiedotDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /opintooikeustiedots` : get all the opintooikeustiedots.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of opintooikeustiedots in body.
     */
    @GetMapping("/opintooikeustiedots")
    fun getAllOpintooikeustiedots(): MutableList<OpintooikeustiedotDTO> {
        log.debug("REST request to get all Opintooikeustiedots")

        return opintooikeustiedotService.findAll()
    }

    /**
     * `GET  /opintooikeustiedots/:id` : get the "id" opintooikeustiedot.
     *
     * @param id the id of the opintooikeustiedotDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the opintooikeustiedotDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/opintooikeustiedots/{id}")
    fun getOpintooikeustiedot(@PathVariable id: Long): ResponseEntity<OpintooikeustiedotDTO> {
        log.debug("REST request to get Opintooikeustiedot : $id")
        val opintooikeustiedotDTO = opintooikeustiedotService.findOne(id)
        return ResponseUtil.wrapOrNotFound(opintooikeustiedotDTO)
    }
    /**
     *  `DELETE  /opintooikeustiedots/:id` : delete the "id" opintooikeustiedot.
     *
     * @param id the id of the opintooikeustiedotDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/opintooikeustiedots/{id}")
    fun deleteOpintooikeustiedot(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Opintooikeustiedot : $id")

        opintooikeustiedotService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
