package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val epaOsaamisalueService: EpaOsaamisalueService,
    private val epaOsaamisalueenKategoriaService: EpaOsaamisalueenKategoriaService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/suoritusarvioinnit-rajaimet")
    fun getSuoritusarvioinnitRajaimet(
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinnitOptionsDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val options = SuoritusarvioinnitOptionsDTO()
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
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!

        return ResponseEntity.ok(
            suoritusarviointiQueryService
                .findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(criteria, id)
        )
    }

    @GetMapping("/arviointipyynto-lomake")
    fun getArviointipyyntoForm(
        principal: Principal?
    ): ResponseEntity<ArviointipyyntoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val form = ArviointipyyntoFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        form.kunnat = kuntaService.findAll().toMutableSet()
        form.erikoisalat = erikoisalaService.findAll().toMutableSet()
        form.epaOsaamisalueenKategoriat = epaOsaamisalueenKategoriaService.findAll().toMutableSet()
        form.kouluttajat =
            kouluttajavaltuutusService.findAllValtuutettuByValtuuttajaKayttajaUserId(id)
                .toMutableSet()

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
                ENTITY_NAME,
                "idexists"
            )
        }
        if (suoritusarviointiDTO.luottamuksenTaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää luottamuksen tasoa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal"
            )
        }
        if (suoritusarviointiDTO.vaativuustaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää vaativuustasoa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal"
            )
        }
        if (suoritusarviointiDTO.sanallinenArviointi != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää sanallista arviointi. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal"
            )
        }
        if (suoritusarviointiDTO.arviointiAika != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arvioinnin aikaa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal"
            )
        }
        if (suoritusarviointiDTO.tyoskentelyjaksoId == null) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                ENTITY_NAME,
                "dataillegal"
            )
        } else {
            val tyoskentelyjakso = tyoskentelyjaksoService
                .findOne(suoritusarviointiDTO.tyoskentelyjaksoId!!, user.id!!)
            val kirjautunutErikoistuvaLaakari =
                erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)

            if (tyoskentelyjakso == null || kirjautunutErikoistuvaLaakari == null) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    ENTITY_NAME,
                    "dataillegal"
                )
            }

            if (tyoskentelyjakso.erikoistuvaLaakariId != kirjautunutErikoistuvaLaakari.id) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    ENTITY_NAME,
                    "dataillegal"
                )
            }
            if (tyoskentelyjakso.alkamispaiva!! > suoritusarviointiDTO.tapahtumanAjankohta!! ||
                (
                    tyoskentelyjakso.paattymispaiva != null &&
                        suoritusarviointiDTO.tapahtumanAjankohta!! > tyoskentelyjakso.paattymispaiva!!
                    )
            ) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua työskentelyjakson väliin.",
                    ENTITY_NAME,
                    "dataillegal"
                )
            }
        }

        suoritusarviointiDTO.pyynnonAika = LocalDate.now(ZoneId.systemDefault())

        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
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
                    applicationName,
                    true,
                    ENTITY_NAME,
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
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString()
                )
            ).build()
    }
}
