package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteetTableDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.util.*
import javax.validation.Valid

private const val ENTITY_NAME = "suoritemerkinta"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritemerkintaResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val oppimistavoitteenKategoriaService: OppimistavoitteenKategoriaService,
    private val suoritemerkintaService: SuoritemerkintaService
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

    @GetMapping("/oppimistavoitteet-taulukko")
    fun getOppimistavoitteetTable(
        principal: Principal?
    ): ResponseEntity<OppimistavoitteetTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val table = OppimistavoitteetTableDTO()

        table.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).let {
                toSortedOppimistavoitteenKategoriat(it)
            }
        table.suoritemerkinnat = suoritemerkintaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(user.id!!).toSet()

        return ResponseEntity.ok(table)
    }

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaForm(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val form = SuoritemerkintaFormDTO()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toSet()
        form.kunnat = kuntaService.findAll().toSet()
        form.erikoisalat = erikoisalaService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toSet()
        form.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).let {
                toSortedOppimistavoitteenKategoriat(it)
            }

        return ResponseEntity.ok(form)
    }

    private fun toSortedOppimistavoitteenKategoriat(oppimistavoitteenKategoriat: List<OppimistavoitteenKategoriaDTO>): SortedSet<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriat.map {
            it.apply {
                oppimistavoitteet = oppimistavoitteet?.sortedBy { oppimistavoite -> oppimistavoite.nimi }?.toSet()
            }
            it
        }.toSortedSet(compareBy { kategoria -> kategoria.jarjestysnumero })
    }
}
