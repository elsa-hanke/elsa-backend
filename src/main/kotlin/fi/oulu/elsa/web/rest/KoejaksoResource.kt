package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.KoejaksoService
import fi.oulu.elsa.service.dto.KoejaksoDTO
import fi.oulu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "koejakso"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Koejakso].
 */
@RestController
@RequestMapping("/api")
class KoejaksoResource(
    private val koejaksoService: KoejaksoService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /koejaksos` : Create a new koejakso.
     *
     * @param koejaksoDTO the koejaksoDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new koejaksoDTO,
     * or with status `400 (Bad Request)` if the koejakso has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/koejaksos")
    fun createKoejakso(@RequestBody koejaksoDTO: KoejaksoDTO): ResponseEntity<KoejaksoDTO> {
        log.debug("REST request to save Koejakso : $koejaksoDTO")
        if (koejaksoDTO.id != null) {
            throw BadRequestAlertException(
                "A new koejakso cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = koejaksoService.save(koejaksoDTO)
        return ResponseEntity.created(URI("/api/koejaksos/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /koejaksos` : Updates an existing koejakso.
     *
     * @param koejaksoDTO the koejaksoDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated koejaksoDTO,
     * or with status `400 (Bad Request)` if the koejaksoDTO is not valid,
     * or with status `500 (Internal Server Error)` if the koejaksoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/koejaksos")
    fun updateKoejakso(@RequestBody koejaksoDTO: KoejaksoDTO): ResponseEntity<KoejaksoDTO> {
        log.debug("REST request to update Koejakso : $koejaksoDTO")
        if (koejaksoDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = koejaksoService.save(koejaksoDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    koejaksoDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /koejaksos` : get all the koejaksos.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of koejaksos in body.
     */
    @GetMapping("/koejaksos")
    fun getAllKoejaksos(): MutableList<KoejaksoDTO> {
        log.debug("REST request to get all Koejaksos")

        return koejaksoService.findAll()
    }

    /**
     * `GET  /koejaksos/:id` : get the "id" koejakso.
     *
     * @param id the id of the koejaksoDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the koejaksoDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/koejaksos/{id}")
    fun getKoejakso(@PathVariable id: Long): ResponseEntity<KoejaksoDTO> {
        log.debug("REST request to get Koejakso : $id")
        val koejaksoDTO = koejaksoService.findOne(id)
        return ResponseUtil.wrapOrNotFound(koejaksoDTO)
    }
    /**
     *  `DELETE  /koejaksos/:id` : delete the "id" koejakso.
     *
     * @param id the id of the koejaksoDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/koejaksos/{id}")
    fun deleteKoejakso(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Koejakso : $id")

        koejaksoService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
