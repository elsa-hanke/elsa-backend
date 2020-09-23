package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.OsoiteService
import fi.oulu.elsa.service.dto.OsoiteDTO
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

private const val ENTITY_NAME = "osoite"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Osoite].
 */
@RestController
@RequestMapping("/api")
class OsoiteResource(
    private val osoiteService: OsoiteService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /osoites` : Create a new osoite.
     *
     * @param osoiteDTO the osoiteDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new osoiteDTO, or with status `400 (Bad Request)` if the osoite has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/osoites")
    fun createOsoite(@Valid @RequestBody osoiteDTO: OsoiteDTO): ResponseEntity<OsoiteDTO> {
        log.debug("REST request to save Osoite : $osoiteDTO")
        if (osoiteDTO.id != null) {
            throw BadRequestAlertException(
                "A new osoite cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = osoiteService.save(osoiteDTO)
        return ResponseEntity.created(URI("/api/osoites/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /osoites` : Updates an existing osoite.
     *
     * @param osoiteDTO the osoiteDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated osoiteDTO,
     * or with status `400 (Bad Request)` if the osoiteDTO is not valid,
     * or with status `500 (Internal Server Error)` if the osoiteDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/osoites")
    fun updateOsoite(@Valid @RequestBody osoiteDTO: OsoiteDTO): ResponseEntity<OsoiteDTO> {
        log.debug("REST request to update Osoite : $osoiteDTO")
        if (osoiteDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = osoiteService.save(osoiteDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    osoiteDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /osoites` : get all the osoites.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of osoites in body.
     */
    @GetMapping("/osoites")
    fun getAllOsoites(): MutableList<OsoiteDTO> {
        log.debug("REST request to get all Osoites")

        return osoiteService.findAll()
    }

    /**
     * `GET  /osoites/:id` : get the "id" osoite.
     *
     * @param id the id of the osoiteDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the osoiteDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/osoites/{id}")
    fun getOsoite(@PathVariable id: Long): ResponseEntity<OsoiteDTO> {
        log.debug("REST request to get Osoite : $id")
        val osoiteDTO = osoiteService.findOne(id)
        return ResponseUtil.wrapOrNotFound(osoiteDTO)
    }
    /**
     *  `DELETE  /osoites/:id` : delete the "id" osoite.
     *
     * @param id the id of the osoiteDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/osoites/{id}")
    fun deleteOsoite(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Osoite : $id")

        osoiteService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
