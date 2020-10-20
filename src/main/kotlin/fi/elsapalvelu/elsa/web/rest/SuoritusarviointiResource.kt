package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.PaginationUtil
import io.github.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.security.Principal

private const val ENTITY_NAME = "suoritusarviointi"

@RestController
@RequestMapping("/api")
class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val userService: UserService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritusarvioinnit")
    fun createSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "A new suoritusarviointi cannot already have an ID",
                ENTITY_NAME, "idexists"
            )
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                     suoritusarviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        pageable: Pageable
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val page = suoritusarviointiService.findAll(pageable)
        val headers = PaginationUtil
            .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)
        return ResponseEntity.ok().headers(headers).body(page.content)
    }

    @GetMapping("/suoritusarvioinnit/omat")
    fun getAllSuoritusarvioinnit(
        principal: Principal?,
        pageable: Pageable
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        if (principal is AbstractAuthenticationToken) {
            val userId = userService.getUserFromAuthentication(principal).id!! // id on olemassa aina
            val page = suoritusarviointiService.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
                userId,
                pageable
            )
            val headers = PaginationUtil
                .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)
            return ResponseEntity.ok().headers(headers).body(page.content)
        } else {
            throw AccountResource.AccountResourceException("User could not be found")
        }
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long
    ): ResponseEntity<SuoritusarviointiDTO> {
        val suoritusarviointiDTO = suoritusarviointiService.findOne(id)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @DeleteMapping("/suoritusarvioinnit/{id}")
    fun deleteSuoritusarviointi(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        suoritusarviointiService.delete(id)
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString()
                )).build()
    }
}
