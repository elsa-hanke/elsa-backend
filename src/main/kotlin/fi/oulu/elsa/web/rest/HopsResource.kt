package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.HopsService
import fi.oulu.elsa.service.dto.HopsDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "hops"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Hops].
 */
@RestController
@RequestMapping("/api")
class HopsResource(
    private val hopsService: HopsService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /hops` : Create a new hops.
     *
     * @param hopsDTO the hopsDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new hopsDTO,
     * or with status `400 (Bad Request)` if the hops has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/hops")
    fun createHops(@RequestBody hopsDTO: HopsDTO): ResponseEntity<HopsDTO> {
        log.debug("REST request to save Hops : $hopsDTO")
        if (hopsDTO.id != null) {
            throw BadRequestAlertException(
                "A new hops cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = hopsService.save(hopsDTO)
        return ResponseEntity.created(URI("/api/hops/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /hops` : Updates an existing hops.
     *
     * @param hopsDTO the hopsDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated hopsDTO,
     * or with status `400 (Bad Request)` if the hopsDTO is not valid,
     * or with status `500 (Internal Server Error)` if the hopsDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/hops")
    fun updateHops(@RequestBody hopsDTO: HopsDTO): ResponseEntity<HopsDTO> {
        log.debug("REST request to update Hops : $hopsDTO")
        if (hopsDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = hopsService.save(hopsDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    hopsDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /hops` : get all the hops.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of hops in body.
     */
    @GetMapping("/hops")
    fun getAllHops(): MutableList<HopsDTO> {
        log.debug("REST request to get all Hops")

        return hopsService.findAll()
    }

    /**
     * `GET  /hops/:id` : get the "id" hops.
     *
     * @param id the id of the hopsDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the hopsDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/hops/{id}")
    fun getHops(@PathVariable id: Long): ResponseEntity<HopsDTO> {
        log.debug("REST request to get Hops : $id")
        val hopsDTO = hopsService.findOne(id)
        return ResponseUtil.wrapOrNotFound(hopsDTO)
    }
    /**
     *  `DELETE  /hops/:id` : delete the "id" hops.
     *
     * @param id the id of the hopsDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/hops/{id}")
    fun deleteHops(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Hops : $id")

        hopsService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
