package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.PikaviestiService
import fi.elsapalvelu.elsa.service.dto.PikaviestiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "pikaviesti"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.Pikaviesti].
 */
@RestController
@RequestMapping("/api")
class PikaviestiResource(
    private val pikaviestiService: PikaviestiService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /pikaviestis` : Create a new pikaviesti.
     *
     * @param pikaviestiDTO the pikaviestiDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new pikaviestiDTO,
     * or with status `400 (Bad Request)` if the pikaviesti has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/pikaviestis")
    fun createPikaviesti(@RequestBody pikaviestiDTO: PikaviestiDTO): ResponseEntity<PikaviestiDTO> {
        log.debug("REST request to save Pikaviesti : $pikaviestiDTO")
        if (pikaviestiDTO.id != null) {
            throw BadRequestAlertException(
                "A new pikaviesti cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = pikaviestiService.save(pikaviestiDTO)
        return ResponseEntity.created(URI("/api/pikaviestis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /pikaviestis` : Updates an existing pikaviesti.
     *
     * @param pikaviestiDTO the pikaviestiDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated pikaviestiDTO,
     * or with status `400 (Bad Request)` if the pikaviestiDTO is not valid,
     * or with status `500 (Internal Server Error)` if the pikaviestiDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/pikaviestis")
    fun updatePikaviesti(@RequestBody pikaviestiDTO: PikaviestiDTO): ResponseEntity<PikaviestiDTO> {
        log.debug("REST request to update Pikaviesti : $pikaviestiDTO")
        if (pikaviestiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = pikaviestiService.save(pikaviestiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    pikaviestiDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /pikaviestis` : get all the pikaviestis.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of pikaviestis in body.
     */
    @GetMapping("/pikaviestis")
    fun getAllPikaviestis(): MutableList<PikaviestiDTO> {
        log.debug("REST request to get all Pikaviestis")

        return pikaviestiService.findAll()
    }

    /**
     * `GET  /pikaviestis/:id` : get the "id" pikaviesti.
     *
     * @param id the id of the pikaviestiDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the pikaviestiDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/pikaviestis/{id}")
    fun getPikaviesti(@PathVariable id: Long): ResponseEntity<PikaviestiDTO> {
        log.debug("REST request to get Pikaviesti : $id")
        val pikaviestiDTO = pikaviestiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(pikaviestiDTO)
    }
    /**
     *  `DELETE  /pikaviestis/:id` : delete the "id" pikaviesti.
     *
     * @param id the id of the pikaviestiDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/pikaviestis/{id}")
    fun deletePikaviesti(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Pikaviesti : $id")

        pikaviestiService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
