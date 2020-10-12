package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.ArviointiosaalueService
import fi.elsapalvelu.elsa.service.dto.ArviointiosaalueDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "arviointiosaalue"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.Arviointiosaalue].
 */
@RestController
@RequestMapping("/api")
class ArviointiosaalueResource(
    private val arviointiosaalueService: ArviointiosaalueService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /arviointiosaalues` : Create a new arviointiosaalue.
     *
     * @param arviointiosaalueDTO the arviointiosaalueDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new arviointiosaalueDTO,
     * or with status `400 (Bad Request)` if the arviointiosaalue has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/arviointiosaalues")
    fun createArviointiosaalue(
        @RequestBody arviointiosaalueDTO: ArviointiosaalueDTO
    ): ResponseEntity<ArviointiosaalueDTO> {
        log.debug("REST request to save Arviointiosaalue : $arviointiosaalueDTO")
        if (arviointiosaalueDTO.id != null) {
            throw BadRequestAlertException(
                "A new arviointiosaalue cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = arviointiosaalueService.save(arviointiosaalueDTO)
        return ResponseEntity.created(URI("/api/arviointiosaalues/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /arviointiosaalues` : Updates an existing arviointiosaalue.
     *
     * @param arviointiosaalueDTO the arviointiosaalueDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated arviointiosaalueDTO,
     * or with status `400 (Bad Request)` if the arviointiosaalueDTO is not valid,
     * or with status `500 (Internal Server Error)` if the arviointiosaalueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/arviointiosaalues")
    fun updateArviointiosaalue(
        @RequestBody arviointiosaalueDTO: ArviointiosaalueDTO
    ): ResponseEntity<ArviointiosaalueDTO> {
        log.debug("REST request to update Arviointiosaalue : $arviointiosaalueDTO")
        if (arviointiosaalueDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = arviointiosaalueService.save(arviointiosaalueDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    arviointiosaalueDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /arviointiosaalues` : get all the arviointiosaalues.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of arviointiosaalues in body.
     */
    @GetMapping("/arviointiosaalues")
    fun getAllArviointiosaalues(): MutableList<ArviointiosaalueDTO> {
        log.debug("REST request to get all Arviointiosaalues")

        return arviointiosaalueService.findAll()
    }

    /**
     * `GET  /arviointiosaalues/:id` : get the "id" arviointiosaalue.
     *
     * @param id the id of the arviointiosaalueDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the arviointiosaalueDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/arviointiosaalues/{id}")
    fun getArviointiosaalue(@PathVariable id: Long): ResponseEntity<ArviointiosaalueDTO> {
        log.debug("REST request to get Arviointiosaalue : $id")
        val arviointiosaalueDTO = arviointiosaalueService.findOne(id)
        return ResponseUtil.wrapOrNotFound(arviointiosaalueDTO)
    }
    /**
     *  `DELETE  /arviointiosaalues/:id` : delete the "id" arviointiosaalue.
     *
     * @param id the id of the arviointiosaalueDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/arviointiosaalues/{id}")
    fun deleteArviointiosaalue(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Arviointiosaalue : $id")

        arviointiosaalueService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
