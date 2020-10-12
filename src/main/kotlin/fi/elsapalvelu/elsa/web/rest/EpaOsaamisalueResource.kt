package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
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

private const val ENTITY_NAME = "epaOsaamisalue"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.EpaOsaamisalue].
 */
@RestController
@RequestMapping("/api")
class EpaOsaamisalueResource(
    private val epaOsaamisalueService: EpaOsaamisalueService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /epa-osaamisalues` : Create a new epaOsaamisalue.
     *
     * @param epaOsaamisalueDTO the epaOsaamisalueDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new epaOsaamisalueDTO, or with status `400 (Bad Request)` if the epaOsaamisalue has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/epa-osaamisalues")
    fun createEpaOsaamisalue(@Valid @RequestBody epaOsaamisalueDTO: EpaOsaamisalueDTO): ResponseEntity<EpaOsaamisalueDTO> {
        log.debug("REST request to save EpaOsaamisalue : $epaOsaamisalueDTO")
        if (epaOsaamisalueDTO.id != null) {
            throw BadRequestAlertException(
                "A new epaOsaamisalue cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = epaOsaamisalueService.save(epaOsaamisalueDTO)
        return ResponseEntity.created(URI("/api/epa-osaamisalues/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /epa-osaamisalues` : Updates an existing epaOsaamisalue.
     *
     * @param epaOsaamisalueDTO the epaOsaamisalueDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated epaOsaamisalueDTO,
     * or with status `400 (Bad Request)` if the epaOsaamisalueDTO is not valid,
     * or with status `500 (Internal Server Error)` if the epaOsaamisalueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/epa-osaamisalues")
    fun updateEpaOsaamisalue(@Valid @RequestBody epaOsaamisalueDTO: EpaOsaamisalueDTO): ResponseEntity<EpaOsaamisalueDTO> {
        log.debug("REST request to update EpaOsaamisalue : $epaOsaamisalueDTO")
        if (epaOsaamisalueDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = epaOsaamisalueService.save(epaOsaamisalueDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     epaOsaamisalueDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /epa-osaamisalues` : get all the epaOsaamisalues.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of epaOsaamisalues in body.
     */
    @GetMapping("/epa-osaamisalues")
    fun getAllEpaOsaamisalues(): MutableList<EpaOsaamisalueDTO> {
        log.debug("REST request to get all EpaOsaamisalues")

        return epaOsaamisalueService.findAll()
            }

    /**
     * `GET  /epa-osaamisalues/:id` : get the "id" epaOsaamisalue.
     *
     * @param id the id of the epaOsaamisalueDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the epaOsaamisalueDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/epa-osaamisalues/{id}")
    fun getEpaOsaamisalue(@PathVariable id: Long): ResponseEntity<EpaOsaamisalueDTO> {
        log.debug("REST request to get EpaOsaamisalue : $id")
        val epaOsaamisalueDTO = epaOsaamisalueService.findOne(id)
        return ResponseUtil.wrapOrNotFound(epaOsaamisalueDTO)
    }
    /**
     *  `DELETE  /epa-osaamisalues/:id` : delete the "id" epaOsaamisalue.
     *
     * @param id the id of the epaOsaamisalueDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/epa-osaamisalues/{id}")
    fun deleteEpaOsaamisalue(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete EpaOsaamisalue : $id")

        epaOsaamisalueService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
