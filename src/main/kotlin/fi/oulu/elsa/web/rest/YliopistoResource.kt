package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.YliopistoService
import fi.oulu.elsa.service.dto.YliopistoDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "yliopisto"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Yliopisto].
 */
@RestController
@RequestMapping("/api")
class YliopistoResource(
    private val yliopistoService: YliopistoService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /yliopistos` : Create a new yliopisto.
     *
     * @param yliopistoDTO the yliopistoDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new yliopistoDTO, or with status `400 (Bad Request)` if the yliopisto has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/yliopistos")
    fun createYliopisto(@RequestBody yliopistoDTO: YliopistoDTO): ResponseEntity<YliopistoDTO> {
        log.debug("REST request to save Yliopisto : $yliopistoDTO")
        if (yliopistoDTO.id != null) {
            throw BadRequestAlertException(
                "A new yliopisto cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = yliopistoService.save(yliopistoDTO)
        return ResponseEntity.created(URI("/api/yliopistos/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /yliopistos` : Updates an existing yliopisto.
     *
     * @param yliopistoDTO the yliopistoDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated yliopistoDTO,
     * or with status `400 (Bad Request)` if the yliopistoDTO is not valid,
     * or with status `500 (Internal Server Error)` if the yliopistoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/yliopistos")
    fun updateYliopisto(@RequestBody yliopistoDTO: YliopistoDTO): ResponseEntity<YliopistoDTO> {
        log.debug("REST request to update Yliopisto : $yliopistoDTO")
        if (yliopistoDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = yliopistoService.save(yliopistoDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    yliopistoDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /yliopistos` : get all the yliopistos.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the [ResponseEntity] with status `200 (OK)` and the list of yliopistos in body.
     */
    @GetMapping("/yliopistos")
    fun getAllYliopistos(@RequestParam(required = false, defaultValue = "false") eagerload: Boolean): MutableList<YliopistoDTO> {
        log.debug("REST request to get all Yliopistos")

        return yliopistoService.findAll()
    }

    /**
     * `GET  /yliopistos/:id` : get the "id" yliopisto.
     *
     * @param id the id of the yliopistoDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the yliopistoDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/yliopistos/{id}")
    fun getYliopisto(@PathVariable id: Long): ResponseEntity<YliopistoDTO> {
        log.debug("REST request to get Yliopisto : $id")
        val yliopistoDTO = yliopistoService.findOne(id)
        return ResponseUtil.wrapOrNotFound(yliopistoDTO)
    }
    /**
     *  `DELETE  /yliopistos/:id` : delete the "id" yliopisto.
     *
     * @param id the id of the yliopistoDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/yliopistos/{id}")
    fun deleteYliopisto(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Yliopisto : $id")

        yliopistoService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
