package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.SuoritusarviointiService
import fi.oulu.elsa.service.dto.SuoritusarviointiDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import java.net.URISyntaxException
import javax.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "suoritusarviointi"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Suoritusarviointi].
 */
@RestController
@RequestMapping("/api")
class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /suoritusarviointis` : Create a new suoritusarviointi.
     *
     * @param suoritusarviointiDTO the suoritusarviointiDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new suoritusarviointiDTO, or with status `400 (Bad Request)` if the suoritusarviointi has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/suoritusarviointis")
    fun createSuoritusarviointi(@Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO): ResponseEntity<SuoritusarviointiDTO> {
        log.debug("REST request to save Suoritusarviointi : $suoritusarviointiDTO")
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new suoritusarviointi cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarviointis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /suoritusarviointis` : Updates an existing suoritusarviointi.
     *
     * @param suoritusarviointiDTO the suoritusarviointiDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated suoritusarviointiDTO,
     * or with status `400 (Bad Request)` if the suoritusarviointiDTO is not valid,
     * or with status `500 (Internal Server Error)` if the suoritusarviointiDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/suoritusarviointis")
    fun updateSuoritusarviointi(@Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO): ResponseEntity<SuoritusarviointiDTO> {
        log.debug("REST request to update Suoritusarviointi : $suoritusarviointiDTO")
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     suoritusarviointiDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /suoritusarviointis` : get all the suoritusarviointis.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of suoritusarviointis in body.
     */
    @GetMapping("/suoritusarviointis")
    fun getAllSuoritusarviointis(): MutableList<SuoritusarviointiDTO> {
        log.debug("REST request to get all Suoritusarviointis")

        return suoritusarviointiService.findAll()
            }

    /**
     * `GET  /suoritusarviointis/:id` : get the "id" suoritusarviointi.
     *
     * @param id the id of the suoritusarviointiDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the suoritusarviointiDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/suoritusarviointis/{id}")
    fun getSuoritusarviointi(@PathVariable id: Long): ResponseEntity<SuoritusarviointiDTO> {
        log.debug("REST request to get Suoritusarviointi : $id")
        val suoritusarviointiDTO = suoritusarviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }
    /**
     *  `DELETE  /suoritusarviointis/:id` : delete the "id" suoritusarviointi.
     *
     * @param id the id of the suoritusarviointiDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/suoritusarviointis/{id}")
    fun deleteSuoritusarviointi(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Suoritusarviointi : $id")

        suoritusarviointiService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
