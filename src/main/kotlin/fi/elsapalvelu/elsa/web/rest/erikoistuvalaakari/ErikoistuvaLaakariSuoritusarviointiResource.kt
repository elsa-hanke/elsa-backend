package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.PaginationUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import java.time.ZoneId
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val epaOsaamisalueService: EpaOsaamisalueService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/suoritusarvioinnit-rajaimet")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinnitOptionsDto> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val options = SuoritusarvioinnitOptionsDto()
        options.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        options.epaOsaamisalueet = epaOsaamisalueService.findAll().toMutableSet()
        options.tapahtumat = suoritusarviointiService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        options.kouluttajat = kouluttajavaltuutusService
            .findAllValtuutettuByValtuuttajaKayttajaUserId(id).toMutableSet()

        return ResponseEntity.ok(options)
    }

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        criteria: SuoritusarviointiCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val page = suoritusarviointiQueryService
            .findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(criteria, id, pageable)
        val headers = PaginationUtil
            .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)

        return ResponseEntity.ok().headers(headers).body(page)
    }

    @GetMapping("/arviointipyynto-lomake")
    fun getArviointipyyntoLomake(
        principal: Principal?
    ): ResponseEntity<ArviointipyyntoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val form = ArviointipyyntoFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        form.epaOsaamisalueet = epaOsaamisalueService.findAll().toMutableSet()
        form.kouluttajat = kouluttajavaltuutusService.findAllValtuutettuByValtuuttajaKayttajaUserId(id).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/suoritusarvioinnit/arviointipyynto")
    fun createSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää ID:tä.",
                ENTITY_NAME, "idexists"
            )
        }
        if (suoritusarviointiDTO.vaativuustaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää vaativuustasoa. Kouluttaja määrittelee sen.",
                ENTITY_NAME, "dataillegal"
            )
        }
        if (suoritusarviointiDTO.sanallinenArviointi != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää sanallista arviointi. Kouluttaja määrittelee sen.",
                ENTITY_NAME, "dataillegal"
            )
        }
        if (suoritusarviointiDTO.arviointiAika != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arvioinnin aikaa. Kouluttaja määrittelee sen.",
                ENTITY_NAME, "dataillegal"
            )
        }
        if (suoritusarviointiDTO.tyoskentelyjaksoId == null) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                ENTITY_NAME, "dataillegal"
            )
        } else {
            val tyoskentelyjakso = tyoskentelyjaksoService.findOne(suoritusarviointiDTO.tyoskentelyjaksoId!!)
            val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
            if (!tyoskentelyjakso.isPresent || !erikoistuvaLaakari.isPresent) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    ENTITY_NAME, "dataillegal"
                )
            }
            if (tyoskentelyjakso.get().erikoistuvaLaakariId!! != erikoistuvaLaakari.get().id) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    ENTITY_NAME, "dataillegal"
                )
            }
            if (tyoskentelyjakso.get().alkamispaiva!! > suoritusarviointiDTO.tapahtumanAjankohta!! ||
                (tyoskentelyjakso.get().paattymispaiva != null &&
                    suoritusarviointiDTO.tapahtumanAjankohta!! > tyoskentelyjakso.get().paattymispaiva!!)
            ) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua työskentelyjakson väliin.",
                    ENTITY_NAME, "dataillegal"
                )
            }
        }

        suoritusarviointiDTO.pyynnonAika = LocalDate.now(ZoneId.systemDefault())

        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString())
            )
            .body(result)
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        val user = userService.getAuthenticatedUser(principal)
        val result = suoritusarviointiService.save(suoritusarviointiDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName, true, ENTITY_NAME,
                    suoritusarviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarviointiDTO = suoritusarviointiService
            .findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @DeleteMapping("/suoritusarvioinnit/{id}")
    fun deleteSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        suoritusarviointiService.delete(id, user.id!!)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(
                applicationName,
                true,
                ENTITY_NAME,
                id.toString()
            )).build()
    }
}