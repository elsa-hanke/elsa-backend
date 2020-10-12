package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.ArvioitavaOsaalueService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import java.net.URISyntaxException
import javax.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "arvioitavaOsaalue"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue].
 */
@RestController
@RequestMapping("/api")
class ArvioitavaOsaalueResource(
    private val arvioitavaOsaalueService: ArvioitavaOsaalueService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /arvioitava-osaalues` : Create a new arvioitavaOsaalue.
     *
     * @param arvioitavaOsaalueDTO the arvioitavaOsaalueDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new arvioitavaOsaalueDTO, or with status `400 (Bad Request)` if the arvioitavaOsaalue has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/arvioitava-osaalues")
    fun createArvioitavaOsaalue(@Valid @RequestBody arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ResponseEntity<ArvioitavaOsaalueDTO> {
        log.debug("REST request to save ArvioitavaOsaalue : $arvioitavaOsaalueDTO")
        if (arvioitavaOsaalueDTO.id != null) {
            throw BadRequestAlertException(
                "A new arvioitavaOsaalue cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = arvioitavaOsaalueService.save(arvioitavaOsaalueDTO)
        return ResponseEntity.created(URI("/api/arvioitava-osaalues/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /arvioitava-osaalues` : Updates an existing arvioitavaOsaalue.
     *
     * @param arvioitavaOsaalueDTO the arvioitavaOsaalueDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated arvioitavaOsaalueDTO,
     * or with status `400 (Bad Request)` if the arvioitavaOsaalueDTO is not valid,
     * or with status `500 (Internal Server Error)` if the arvioitavaOsaalueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/arvioitava-osaalues")
    fun updateArvioitavaOsaalue(@Valid @RequestBody arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ResponseEntity<ArvioitavaOsaalueDTO> {
        log.debug("REST request to update ArvioitavaOsaalue : $arvioitavaOsaalueDTO")
        if (arvioitavaOsaalueDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = arvioitavaOsaalueService.save(arvioitavaOsaalueDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     arvioitavaOsaalueDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /arvioitava-osaalues` : get all the arvioitavaOsaalues.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of arvioitavaOsaalues in body.
     */
    @GetMapping("/arvioitava-osaalues")
    fun getAllArvioitavaOsaalues(): MutableList<ArvioitavaOsaalueDTO> {
        log.debug("REST request to get all ArvioitavaOsaalues")

        return arvioitavaOsaalueService.findAll()
            }

    /**
     * `GET  /arvioitava-osaalues/:id` : get the "id" arvioitavaOsaalue.
     *
     * @param id the id of the arvioitavaOsaalueDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the arvioitavaOsaalueDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/arvioitava-osaalues/{id}")
    fun getArvioitavaOsaalue(@PathVariable id: Long): ResponseEntity<ArvioitavaOsaalueDTO> {
        log.debug("REST request to get ArvioitavaOsaalue : $id")
        val arvioitavaOsaalueDTO = arvioitavaOsaalueService.findOne(id)
        return ResponseUtil.wrapOrNotFound(arvioitavaOsaalueDTO)
    }
    /**
     *  `DELETE  /arvioitava-osaalues/:id` : delete the "id" arvioitavaOsaalue.
     *
     * @param id the id of the arvioitavaOsaalueDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/arvioitava-osaalues/{id}")
    fun deleteArvioitavaOsaalue(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete ArvioitavaOsaalue : $id")

        arvioitavaOsaalueService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
