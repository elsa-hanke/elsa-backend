package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.OsaamisenArviointiService
import fi.elsapalvelu.elsa.service.dto.OsaamisenArviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

private const val ENTITY_NAME = "osaamisenArviointi"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.OsaamisenArviointi].
 */
@RestController
@RequestMapping("/api")
class OsaamisenArviointiResource(
    private val osaamisenArviointiService: OsaamisenArviointiService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /osaamisen-arviointis` : Create a new osaamisenArviointi.
     *
     * @param osaamisenArviointiDTO the osaamisenArviointiDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new osaamisenArviointiDTO,
     * or with status `400 (Bad Request)` if the osaamisenArviointi has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/osaamisen-arviointis")
    fun createOsaamisenArviointi(
        @RequestBody osaamisenArviointiDTO: OsaamisenArviointiDTO
    ): ResponseEntity<OsaamisenArviointiDTO> {
        log.debug("REST request to save OsaamisenArviointi : $osaamisenArviointiDTO")
        if (osaamisenArviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new osaamisenArviointi cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        val result = osaamisenArviointiService.save(osaamisenArviointiDTO)
        return ResponseEntity.created(URI("/api/osaamisen-arviointis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /osaamisen-arviointis` : Updates an existing osaamisenArviointi.
     *
     * @param osaamisenArviointiDTO the osaamisenArviointiDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated osaamisenArviointiDTO,
     * or with status `400 (Bad Request)` if the osaamisenArviointiDTO is not valid,
     * or with status `500 (Internal Server Error)` if the osaamisenArviointiDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/osaamisen-arviointis")
    fun updateOsaamisenArviointi(
        @RequestBody osaamisenArviointiDTO: OsaamisenArviointiDTO
    ): ResponseEntity<OsaamisenArviointiDTO> {
        log.debug("REST request to update OsaamisenArviointi : $osaamisenArviointiDTO")
        if (osaamisenArviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = osaamisenArviointiService.save(osaamisenArviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    osaamisenArviointiDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /osaamisen-arviointis` : get all the osaamisenArviointis.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of osaamisenArviointis in body.
     */
    @GetMapping("/osaamisen-arviointis")
    fun getAllOsaamisenArviointis(): MutableList<OsaamisenArviointiDTO> {
        log.debug("REST request to get all OsaamisenArviointis")

        return osaamisenArviointiService.findAll()
    }

    /**
     * `GET  /osaamisen-arviointis/:id` : get the "id" osaamisenArviointi.
     *
     * @param id the id of the osaamisenArviointiDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the osaamisenArviointiDTO,
     * or with status `404 (Not Found)`.
     */
    @GetMapping("/osaamisen-arviointis/{id}")
    fun getOsaamisenArviointi(@PathVariable id: Long): ResponseEntity<OsaamisenArviointiDTO> {
        log.debug("REST request to get OsaamisenArviointi : $id")
        val osaamisenArviointiDTO = osaamisenArviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(osaamisenArviointiDTO)
    }
    /**
     *  `DELETE  /osaamisen-arviointis/:id` : delete the "id" osaamisenArviointi.
     *
     * @param id the id of the osaamisenArviointiDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/osaamisen-arviointis/{id}")
    fun deleteOsaamisenArviointi(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete OsaamisenArviointi : $id")

        osaamisenArviointiService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
