package fi.elsapalvelu.elsa.web.rest.common

import java.time.Clock
import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.security.Principal
import fi.elsapalvelu.elsa.service.arviointi.SuoritusarvioinninKommenttiService
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Instant
import jakarta.validation.Valid

private const val ENTITY_NAME = "suoritusarvioinnin_kommentti"

@RestController
@PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
@RequestMapping("/api/")
class ErikoistuvaLaakariSuoritusarvioinninKommenttiResource(
    private val suoritusarvioinninKommenttiService: SuoritusarvioinninKommenttiService,
    private val userService: UserService,
    private val clock: Clock
) {

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
        val nykyhetki = Instant.now(clock)
        suoritusarvioinninKommenttiDTO.luontiaika = nykyhetki
        suoritusarvioinninKommenttiDTO.muokkausaika = nykyhetki
        suoritusarvioinninKommenttiDTO.suoritusarviointiId = id
        val result = suoritusarvioinninKommenttiService
            .save(suoritusarvioinninKommenttiDTO, user.id.required())

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
        suoritusarvioinninKommenttiDTO.muokkausaika = Instant.now(clock)
        val result =
            suoritusarvioinninKommenttiService.save(suoritusarvioinninKommenttiDTO, user.id.required())
        return ResponseEntity.ok(result)
    }
}
