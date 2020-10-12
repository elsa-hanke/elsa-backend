package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.OsaalueenArviointiService
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
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

private const val ENTITY_NAME = "osaalueenArviointi"
/**
 * REST controller for managing [fi.elsapalvelu.elsa.domain.OsaalueenArviointi].
 */
@RestController
@RequestMapping("/api")
class OsaalueenArviointiResource(
    private val osaalueenArviointiService: OsaalueenArviointiService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    /**
     * `POST  /osaalueen-arviointis` : Create a new osaalueenArviointi.
     *
     * @param osaalueenArviointiDTO the osaalueenArviointiDTO to create.
     * @return the [ResponseEntity] with status `201 (Created)` and with body the new osaalueenArviointiDTO, or with status `400 (Bad Request)` if the osaalueenArviointi has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/osaalueen-arviointis")
    fun createOsaalueenArviointi(@Valid @RequestBody osaalueenArviointiDTO: OsaalueenArviointiDTO): ResponseEntity<OsaalueenArviointiDTO> {
        log.debug("REST request to save OsaalueenArviointi : $osaalueenArviointiDTO")
        if (osaalueenArviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new osaalueenArviointi cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = osaalueenArviointiService.save(osaalueenArviointiDTO)
        return ResponseEntity.created(URI("/api/osaalueen-arviointis/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * `PUT  /osaalueen-arviointis` : Updates an existing osaalueenArviointi.
     *
     * @param osaalueenArviointiDTO the osaalueenArviointiDTO to update.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the updated osaalueenArviointiDTO,
     * or with status `400 (Bad Request)` if the osaalueenArviointiDTO is not valid,
     * or with status `500 (Internal Server Error)` if the osaalueenArviointiDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/osaalueen-arviointis")
    fun updateOsaalueenArviointi(@Valid @RequestBody osaalueenArviointiDTO: OsaalueenArviointiDTO): ResponseEntity<OsaalueenArviointiDTO> {
        log.debug("REST request to update OsaalueenArviointi : $osaalueenArviointiDTO")
        if (osaalueenArviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = osaalueenArviointiService.save(osaalueenArviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     osaalueenArviointiDTO.id.toString()
                )
            )
            .body(result)
    }
    /**
     * `GET  /osaalueen-arviointis` : get all the osaalueenArviointis.
     *

     * @return the [ResponseEntity] with status `200 (OK)` and the list of osaalueenArviointis in body.
     */
    @GetMapping("/osaalueen-arviointis")
    fun getAllOsaalueenArviointis(): MutableList<OsaalueenArviointiDTO> {
        log.debug("REST request to get all OsaalueenArviointis")

        return osaalueenArviointiService.findAll()
            }

    /**
     * `GET  /osaalueen-arviointis/:id` : get the "id" osaalueenArviointi.
     *
     * @param id the id of the osaalueenArviointiDTO to retrieve.
     * @return the [ResponseEntity] with status `200 (OK)` and with body the osaalueenArviointiDTO, or with status `404 (Not Found)`.
     */
    @GetMapping("/osaalueen-arviointis/{id}")
    fun getOsaalueenArviointi(@PathVariable id: Long): ResponseEntity<OsaalueenArviointiDTO> {
        log.debug("REST request to get OsaalueenArviointi : $id")
        val osaalueenArviointiDTO = osaalueenArviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(osaalueenArviointiDTO)
    }
    /**
     *  `DELETE  /osaalueen-arviointis/:id` : delete the "id" osaalueenArviointi.
     *
     * @param id the id of the osaalueenArviointiDTO to delete.
     * @return the [ResponseEntity] with status `204 (NO_CONTENT)`.
     */
    @DeleteMapping("/osaalueen-arviointis/{id}")
    fun deleteOsaalueenArviointi(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("REST request to delete OsaalueenArviointi : $id")

        osaalueenArviointiService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }
}
