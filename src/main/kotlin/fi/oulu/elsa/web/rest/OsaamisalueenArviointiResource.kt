package fi.oulu.elsa.web.rest

import fi.oulu.elsa.service.OsaamisalueenArviointiService
import fi.oulu.elsa.service.dto.OsaamisalueenArviointiDTO
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

private const val ENTITY_NAME = "osaamisalueenArviointi"
/**
 * REST controller for managing [fi.oulu.elsa.domain.OsaamisalueenArviointi].
 */
@RestController
@RequestMapping("/api")
class OsaamisalueenArviointiResource(
    private val osaamisalueenArviointiService: OsaamisalueenArviointiService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /osaamisalueen-arviointis` : Create a new osaamisalueenArviointi.
     *
     * @param osaamisalueenArviointiDTO the osaamisalueenArviointiDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new osaamisalueenArviointiDTO,
     * or with status `400 (Bad Request)` if the osaamisalueenArviointi has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/osaamisalueen-arviointis")
    fun createOsaamisalueenArviointi(
        @Valid @RequestBody osaamisalueenArviointiDTO: OsaamisalueenArviointiDTO
    ): ResponseEntity<OsaamisalueenArviointiDTO> {
        log.debug("REST request to save OsaamisalueenArviointi : $osaamisalueenArviointiDTO")
        if (osaamisalueenArviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new osaamisalueenArviointi cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = osaamisalueenArviointiService.save(osaamisalueenArviointiDTO)
        return ResponseEntity.created(URI("/api/osaamisalueen-arviointis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /osaamisalueen-arviointis` : Updates an existing osaamisalueenArviointi.
     *
     * @param osaamisalueenArviointiDTO the osaamisalueenArviointiDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated osaamisalueenArviointiDTO,
     * or with status `400 (Bad Request)` if the osaamisalueenArviointiDTO is not valid,
     * or with status `500 (Internal Server Error)` if the osaamisalueenArviointiDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/osaamisalueen-arviointis")
    fun updateOsaamisalueenArviointi(
        @Valid @RequestBody osaamisalueenArviointiDTO: OsaamisalueenArviointiDTO
    ): ResponseEntity<OsaamisalueenArviointiDTO> {
        log.debug("REST request to update OsaamisalueenArviointi : $osaamisalueenArviointiDTO")
        if (osaamisalueenArviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = osaamisalueenArviointiService.save(osaamisalueenArviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    osaamisalueenArviointiDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /osaamisalueen-arviointis` : get all the osaamisalueenArviointis.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of osaamisalueenArviointis in body.
     */
    @GetMapping("/osaamisalueen-arviointis")
    fun getAllOsaamisalueenArviointis(): MutableList<OsaamisalueenArviointiDTO> {
        log.debug("REST request to get all OsaamisalueenArviointis")

        return osaamisalueenArviointiService.findAll()
    }

    /**
     * `GET  /osaamisalueen-arviointis/:id` : get the "id" osaamisalueenArviointi.
     *
     * @param id the id of the osaamisalueenArviointiDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the osaamisalueenArviointiDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/osaamisalueen-arviointis/{id}")
    fun getOsaamisalueenArviointi(@PathVariable id: Long): ResponseEntity<OsaamisalueenArviointiDTO> {
        log.debug("REST request to get OsaamisalueenArviointi : $id")
        val osaamisalueenArviointiDTO = osaamisalueenArviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(osaamisalueenArviointiDTO)
    }
    /**
     *  `DELETE  /osaamisalueen-arviointis/:id` : delete the "id" osaamisalueenArviointi.
     *
     * @param id the id of the osaamisalueenArviointiDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/osaamisalueen-arviointis/{id}")
    fun deleteOsaamisalueenArviointi(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete OsaamisalueenArviointi : $id")

        osaamisalueenArviointiService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
