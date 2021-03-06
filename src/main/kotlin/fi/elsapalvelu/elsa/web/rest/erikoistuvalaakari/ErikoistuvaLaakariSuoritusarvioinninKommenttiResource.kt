package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import java.time.Instant
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarvioinnin_kommentti"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritusarvioinninKommenttiResource(
    private val suoritusarvioinninKommenttiService: SuoritusarvioinninKommenttiService,
    private val userService: UserService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritusarvioinnit/{id}/kommentti")
    fun createSuoritusarvioinninKommentti(
        @PathVariable id: Long,
        @Valid @RequestBody suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinninKommenttiDTO> {
        if (suoritusarvioinninKommenttiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi suoritusarvioinnin kommentti ei saa sisältää ID:tä.",
                ENTITY_NAME,
                "idexists"
            )
        }
        val user = userService.getAuthenticatedUser(principal)
        val now = Instant.now()
        suoritusarvioinninKommenttiDTO.luontiaika = now
        suoritusarvioinninKommenttiDTO.muokkausaika = now
        suoritusarvioinninKommenttiDTO.suoritusarviointiId = id
        val result = suoritusarvioinninKommenttiService
            .save(suoritusarvioinninKommenttiDTO, user.login!!)

        return ResponseEntity.created(URI("/api/suoritusarvioinnit/$id/kommentti/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/suoritusarvioinnit/{id}/kommentti")
    fun updateSuoritusarvioinninKommentti(
        @Valid @RequestBody suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinninKommenttiDTO> {
        if (suoritusarvioinninKommenttiDTO.id == null) {
            throw BadRequestAlertException("Invalid id", "suoritusarvioinnin_kommentti", "idnull")
        }
        val user = userService.getAuthenticatedUser(principal)
        suoritusarvioinninKommenttiDTO.muokkausaika = Instant.now()
        val result =
            suoritusarvioinninKommenttiService.save(suoritusarvioinninKommenttiDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    suoritusarvioinninKommenttiDTO.id.toString()
                )
            )
            .body(result)
    }
}
