package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.KayttajaService
import fi.oulu.elsa.service.dto.KayttajaDTO
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

private const val ENTITY_NAME = "kayttaja"
/**
 * REST controller for managing [fi.oulu.elsa.domain.Kayttaja].
 */
@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val kayttajaService: KayttajaService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /kayttajas` : Create a new kayttaja.
     *
     * @param kayttajaDTO the kayttajaDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new kayttajaDTO,
     * or with status `400 (Bad Request)` if the kayttaja has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/kayttajas")
    fun createKayttaja(@Valid @RequestBody kayttajaDTO: KayttajaDTO): ResponseEntity<KayttajaDTO> {
        log.debug("REST request to save Kayttaja : $kayttajaDTO")
        if (kayttajaDTO.id != null) {
            throw BadRequestAlertException(
                "A new kayttaja cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = kayttajaService.save(kayttajaDTO)
        return ResponseEntity.created(URI("/api/kayttajas/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /kayttajas` : Updates an existing kayttaja.
     *
     * @param kayttajaDTO the kayttajaDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated kayttajaDTO,
     * or with status `400 (Bad Request)` if the kayttajaDTO is not valid,
     * or with status `500 (Internal Server Error)` if the kayttajaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/kayttajas")
    fun updateKayttaja(@Valid @RequestBody kayttajaDTO: KayttajaDTO): ResponseEntity<KayttajaDTO> {
        log.debug("REST request to update Kayttaja : $kayttajaDTO")
        if (kayttajaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = kayttajaService.save(kayttajaDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    kayttajaDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /kayttajas` : get all the kayttajas.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of kayttajas in body.
     */
    @GetMapping("/kayttajas")
    fun getAllKayttajas(): MutableList<KayttajaDTO> {
        log.debug("REST request to get all Kayttajas")

        return kayttajaService.findAll()
    }

    /**
     * `GET  /kayttajas/:id` : get the "id" kayttaja.
     *
     * @param id the id of the kayttajaDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the kayttajaDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/kayttajas/{id}")
    fun getKayttaja(@PathVariable id: Long): ResponseEntity<KayttajaDTO> {
        log.debug("REST request to get Kayttaja : $id")
        val kayttajaDTO = kayttajaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(kayttajaDTO)
    }
    /**
     *  `DELETE  /kayttajas/:id` : delete the "id" kayttaja.
     *
     * @param id the id of the kayttajaDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/kayttajas/{id}")
    fun deleteKayttaja(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete Kayttaja : $id")

        kayttajaService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
