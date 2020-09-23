package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.PikaviestiKeskusteluService
import fi.oulu.elsa.service.dto.PikaviestiKeskusteluDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "pikaviestiKeskustelu"
/**
 * REST controller for managing [fi.oulu.elsa.domain.PikaviestiKeskustelu].
 */
@RestController
@RequestMapping("/api")
class PikaviestiKeskusteluResource(
    private val pikaviestiKeskusteluService: PikaviestiKeskusteluService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /pikaviesti-keskustelus` : Create a new pikaviestiKeskustelu.
     *
     * @param pikaviestiKeskusteluDTO the pikaviestiKeskusteluDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new pikaviestiKeskusteluDTO,
     * or with status `400 (Bad Request)` if the pikaviestiKeskustelu has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/pikaviesti-keskustelus")
    fun createPikaviestiKeskustelu(
        @RequestBody pikaviestiKeskusteluDTO: PikaviestiKeskusteluDTO
    ): ResponseEntity<PikaviestiKeskusteluDTO> {
        log.debug("REST request to save PikaviestiKeskustelu : $pikaviestiKeskusteluDTO")
        if (pikaviestiKeskusteluDTO.id != null) {
            throw BadRequestAlertException(
                "A new pikaviestiKeskustelu cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = pikaviestiKeskusteluService.save(pikaviestiKeskusteluDTO)
        return ResponseEntity.created(URI("/api/pikaviesti-keskustelus/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /pikaviesti-keskustelus` : Updates an existing pikaviestiKeskustelu.
     *
     * @param pikaviestiKeskusteluDTO the pikaviestiKeskusteluDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated pikaviestiKeskusteluDTO,
     * or with status `400 (Bad Request)` if the pikaviestiKeskusteluDTO is not valid,
     * or with status `500 (Internal Server Error)` if the pikaviestiKeskusteluDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/pikaviesti-keskustelus")
    fun updatePikaviestiKeskustelu(
        @RequestBody pikaviestiKeskusteluDTO: PikaviestiKeskusteluDTO
    ): ResponseEntity<PikaviestiKeskusteluDTO> {
        log.debug("REST request to update PikaviestiKeskustelu : $pikaviestiKeskusteluDTO")
        if (pikaviestiKeskusteluDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = pikaviestiKeskusteluService.save(pikaviestiKeskusteluDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    pikaviestiKeskusteluDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /pikaviesti-keskustelus` : get all the pikaviestiKeskustelus.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the [ResponseEntity] with status `200 (OK)` and the list of pikaviestiKeskustelus in body.
     */
    @GetMapping("/pikaviesti-keskustelus")
    fun getAllPikaviestiKeskustelus(
        @RequestParam(required = false, defaultValue = "false") eagerload: Boolean
    ): MutableList<PikaviestiKeskusteluDTO> {
        log.debug("REST request to get all PikaviestiKeskustelus")

        return pikaviestiKeskusteluService.findAll()
    }

    /**
     * `GET  /pikaviesti-keskustelus/:id` : get the "id" pikaviestiKeskustelu.
     *
     * @param id the id of the pikaviestiKeskusteluDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the pikaviestiKeskusteluDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/pikaviesti-keskustelus/{id}")
    fun getPikaviestiKeskustelu(@PathVariable id: Long): ResponseEntity<PikaviestiKeskusteluDTO> {
        log.debug("REST request to get PikaviestiKeskustelu : $id")
        val pikaviestiKeskusteluDTO = pikaviestiKeskusteluService.findOne(id)
        return ResponseUtil.wrapOrNotFound(pikaviestiKeskusteluDTO)
    }
    /**
     *  `DELETE  /pikaviesti-keskustelus/:id` : delete the "id" pikaviestiKeskustelu.
     *
     * @param id the id of the pikaviestiKeskusteluDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/pikaviesti-keskustelus/{id}")
    fun deletePikaviestiKeskustelu(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete PikaviestiKeskustelu : $id")

        pikaviestiKeskusteluService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
