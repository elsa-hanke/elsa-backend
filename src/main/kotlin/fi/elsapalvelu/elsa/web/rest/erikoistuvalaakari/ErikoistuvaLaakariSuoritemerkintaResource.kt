package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaFormDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteetTableDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "suoritemerkinta"

@RestController
@PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritemerkintaResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val suoritteenKategoriaService: SuoritteenKategoriaService,
    private val suoritemerkintaService: SuoritemerkintaService,
    private val arviointiasteikkoService: ArviointiasteikkoService,
    private val opintooikeusService: OpintooikeusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritemerkinnat")
    fun createSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        if (suoritemerkintaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi suoritemerkinta ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }

        // Uutta suoritemerkintää ei voi luoda lukittuna
        suoritemerkintaDTO.lukittu = false

        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        suoritemerkintaDTO.arviointiasteikko =
            arviointiasteikkoService.findByOpintooikeusId(opintooikeusId)
        suoritemerkintaService.save(suoritemerkintaDTO, user.id!!)?.let {
            return ResponseEntity
                .created(URI("/api/suoritemerkinnat/${it.id}"))
                .body(it)
        } ?: throw BadRequestAlertException(
            "Uuden suoritemerkinnän työskentelyjakso täytyy olla oma.",
            ENTITY_NAME,
            "dataillegal.uuden-suoritemerkinnan-tyoskentelyjakso-taytyy-olla-oma"
        )
    }

    @PutMapping("/suoritemerkinnat")
    fun updateSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        if (suoritemerkintaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        if (suoritemerkintaDTO.arviointiasteikko != null) {
            throw IllegalArgumentException("Käytettyä arviointiasteikkoa ei voi muokata")
        }

        suoritemerkintaDTO.lukittu = false
        val user = userService.getAuthenticatedUser(principal)

        suoritemerkintaService.save(suoritemerkintaDTO, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw BadRequestAlertException(
            "Suoritemerkinnän työskentelyjakso täytyy olla oma.",
            ENTITY_NAME,
            "dataillegal.suoritemerkinnan-tyoskentelyjakso-taytyy-olla-oma"
        )
    }

    @GetMapping("/suoritemerkinnat/{id}")
    fun getSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        suoritemerkintaService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/suoritemerkinnat/{id}")
    fun deleteSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        suoritemerkintaService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/suoritteet-taulukko")
    fun getSuoritteetTable(
        principal: Principal?
    ): ResponseEntity<SuoritteetTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val table = SuoritteetTableDTO()

        table.suoritteenKategoriat = suoritteenKategoriaService
            .findAllByOpintooikeusId(opintooikeusId).let {
                toSortedSuoritteenKategoriat(it)
            }
        table.suoritemerkinnat = suoritemerkintaService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).toSet()

        table.arviointiasteikko = arviointiasteikkoService.findByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(table)
    }

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaForm(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = SuoritemerkintaFormDTO()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toSet()
        form.kunnat = kuntaService.findAll().toSet()
        form.erikoisalat = erikoisalaService.findAll().toSet()
        form.suoritteenKategoriat = suoritteenKategoriaService
            .findAllByOpintooikeusId(opintooikeusId).let {
                toSortedSuoritteenKategoriat(it)
            }
        form.arviointiasteikko = arviointiasteikkoService.findByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(form)
    }

    private fun toSortedSuoritteenKategoriat(suoritteenKategoriat: List<SuoritteenKategoriaDTO>): Set<SuoritteenKategoriaDTO> {
        return suoritteenKategoriat.map {
            it.apply {
                suoritteet = suoritteet?.sortedBy { suoritteet -> suoritteet.nimi }?.toSet()
            }
            it
        }.sortedBy { kategoria -> kategoria.jarjestysnumero }.toSet()
    }
}
