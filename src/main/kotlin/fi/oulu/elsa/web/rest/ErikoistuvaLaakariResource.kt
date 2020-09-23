package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.ErikoistuvaLaakariService
import fi.oulu.elsa.service.dto.ErikoistuvaLaakariDTO
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

private const val ENTITY_NAME = "erikoistuvaLaakari"
/**
 * REST controller for managing [fi.oulu.elsa.domain.ErikoistuvaLaakari].
 */
@RestController
@RequestMapping("/api")
class ErikoistuvaLaakariResource(
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /erikoistuva-laakaris` : Create a new erikoistuvaLaakari.
     *
     * @param erikoistuvaLaakariDTO the erikoistuvaLaakariDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new erikoistuvaLaakariDTO,
     * or with status `400 (Bad Request)` if the erikoistuvaLaakari has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/erikoistuva-laakaris")
    fun createErikoistuvaLaakari(
        @Valid @RequestBody erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        log.debug("REST request to save ErikoistuvaLaakari : $erikoistuvaLaakariDTO")
        if (erikoistuvaLaakariDTO.id != null) {
            throw BadRequestAlertException(
                "A new erikoistuvaLaakari cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = erikoistuvaLaakariService.save(erikoistuvaLaakariDTO)
        return ResponseEntity.created(URI("/api/erikoistuva-laakaris/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /erikoistuva-laakaris` : Updates an existing erikoistuvaLaakari.
     *
     * @param erikoistuvaLaakariDTO the erikoistuvaLaakariDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated erikoistuvaLaakariDTO,
     * or with status `400 (Bad Request)` if the erikoistuvaLaakariDTO is not valid,
     * or with status `500 (Internal Server Error)` if the erikoistuvaLaakariDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/erikoistuva-laakaris")
    fun updateErikoistuvaLaakari(
        @Valid @RequestBody erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        log.debug("REST request to update ErikoistuvaLaakari : $erikoistuvaLaakariDTO")
        if (erikoistuvaLaakariDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = erikoistuvaLaakariService.save(erikoistuvaLaakariDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    erikoistuvaLaakariDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /erikoistuva-laakaris` : get all the erikoistuvaLaakaris.
     *

     * @param filter the filter of the request.
     * @return the [ResponseEntity] with status `200 (OK)` and the list of erikoistuvaLaakaris in body.
     */
    @GetMapping("/erikoistuva-laakaris")
    fun getAllErikoistuvaLaakaris(@RequestParam(required = false) filter: String?): MutableList<ErikoistuvaLaakariDTO> {
        if ("hops-is-null".equals(filter)) {
            log.debug("REST request to get all ErikoistuvaLaakaris where hops is null")
            return erikoistuvaLaakariService.findAllWhereHopsIsNull()
        }
        if ("koejakso-is-null".equals(filter)) {
            log.debug("REST request to get all ErikoistuvaLaakaris where koejakso is null")
            return erikoistuvaLaakariService.findAllWhereKoejaksoIsNull()
        }
        log.debug("REST request to get all ErikoistuvaLaakaris")

        return erikoistuvaLaakariService.findAll()
    }

    /**
     * `GET  /erikoistuva-laakaris/:id` : get the "id" erikoistuvaLaakari.
     *
     * @param id the id of the erikoistuvaLaakariDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the erikoistuvaLaakariDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/erikoistuva-laakaris/{id}")
    fun getErikoistuvaLaakari(@PathVariable id: Long): ResponseEntity<ErikoistuvaLaakariDTO> {
        log.debug("REST request to get ErikoistuvaLaakari : $id")
        val erikoistuvaLaakariDTO = erikoistuvaLaakariService.findOne(id)
        return ResponseUtil.wrapOrNotFound(erikoistuvaLaakariDTO)
    }
    /**
     *  `DELETE  /erikoistuva-laakaris/:id` : delete the "id" erikoistuvaLaakari.
     *
     * @param id the id of the erikoistuvaLaakariDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/erikoistuva-laakaris/{id}")
    fun deleteErikoistuvaLaakari(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete ErikoistuvaLaakari : $id")

        erikoistuvaLaakariService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
