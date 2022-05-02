package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.SuoritusarvioinninKommenttiService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import java.time.Instant
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarvioinnin_kommentti"

@RestController
@PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
@RequestMapping("/api/")
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
                "Uusi suoritusarvioinnin kommentti ei saa sisältää id:tä",
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
            .save(suoritusarvioinninKommenttiDTO, user.id!!)

        return ResponseEntity.created(URI("/api/suoritusarvioinnit/$id/kommentti/${result.id}"))
            .body(result)
    }

    @PutMapping("/suoritusarvioinnit/{id}/kommentti")
    fun updateSuoritusarvioinninKommentti(
        @Valid @RequestBody suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinninKommenttiDTO> {
        if (suoritusarvioinninKommenttiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        val user = userService.getAuthenticatedUser(principal)
        suoritusarvioinninKommenttiDTO.muokkausaika = Instant.now()
        val result =
            suoritusarvioinninKommenttiService.save(suoritusarvioinninKommenttiDTO, user.id!!)
        return ResponseEntity.ok(result)
    }
}
