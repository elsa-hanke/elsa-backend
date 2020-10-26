package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiPyyntolomakeDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.PaginationUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import java.time.ZoneId
import javax.validation.Valid

@RestController
@RequestMapping("/api")
class RooliErikoistuvaLaakariResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/erikoistuva-laakari")
    fun getErikoistuvaLaakari(
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        if (principal is AbstractAuthenticationToken) {
            val user = userService.getUserFromAuthentication(principal)
            return ResponseUtil.wrapOrNotFound(erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!))
        } else {
            throw AccountResource.AccountResourceException("User could not be found")
        }
    }

    @GetMapping("/erikoistuva-laakari/{erikoistuvaLaakariId}/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        @PathVariable erikoistuvaLaakariId: Long,
        pageable: Pageable
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val page = suoritusarviointiService.findAllByErikoistuvaLaakariId(
            erikoistuvaLaakariId,
            pageable
        )
        val headers = PaginationUtil
            .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)
        return ResponseEntity.ok().headers(headers).body(page.content)
    }

    @GetMapping("/erikoistuva-laakari/{erikoistuvaLaakariId}/arviointipyynto-lomake")
    fun getSuoritusarviointiPyyntolomake(
        @PathVariable erikoistuvaLaakariId: Long
    ): ResponseEntity<SuoritusarviointiPyyntolomakeDTO> {
        val suoritusarviointiPyyntolomakeDTO = SuoritusarviointiPyyntolomakeDTO()
        suoritusarviointiPyyntolomakeDTO.tyoskentelyjaksot = tyoskentelyjaksoService.findAll().toMutableSet()
        suoritusarviointiPyyntolomakeDTO.kouluttajat = kayttajaService.findAll().toMutableSet()

        return ResponseEntity.ok(suoritusarviointiPyyntolomakeDTO)
    }

    @PostMapping("/erikoistuva-laakari/{erikoistuvaLaakariId}/suoritusarvioinnit/arviointipyynto")
    fun createArviointipyynto(
        @PathVariable erikoistuvaLaakariId: Long,
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää ID:tä",
                "suoritusarviointi", "idexists"
            )
        }
        if (suoritusarviointiDTO.vaativuustaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää vaativuustasoa. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        if (suoritusarviointiDTO.sanallinenArvio != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää sanallista arviointi. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        if (suoritusarviointiDTO.arviointiAika != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arvioinnin aikaa. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        suoritusarviointiDTO.pyynnonAika = LocalDate.now(ZoneId.systemDefault()) // Todo: tarkista aikavyöhyke

        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                "suoritusarviointi",
                result.id.toString())
            )
            .body(result)
    }

    @PostMapping("/erikoistuva-laakari/{erikoistuvaLaakariId}/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @PathVariable erikoistuvaLaakariId: Long,
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "A new tyoskentelyjakso cannot already have an ID",
                "tyoskentelyjakso",
                "idexists"
            )
        }
        tyoskentelyjaksoDTO.erikoistuvaLaakariId = erikoistuvaLaakariId
        val result = tyoskentelyjaksoService.save(tyoskentelyjaksoDTO)
        return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                "tyoskentelyjakso",
                result.id.toString())
            )
            .body(result)
    }
}
