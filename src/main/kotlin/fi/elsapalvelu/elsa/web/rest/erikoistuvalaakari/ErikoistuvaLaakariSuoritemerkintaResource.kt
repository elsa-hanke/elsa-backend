package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteetTableDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
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

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritemerkinnat")
    fun createSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        log.debug("REST request to create Suoritemerkinta : $suoritemerkintaDTO")

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
            return ResponseEntity.created(URI("/api/suoritemerkinnat/${it.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        it.id.toString()
                    )
                )
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
        log.debug("REST request to update Suoritemerkinta : $suoritemerkintaDTO")

        if (suoritemerkintaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        suoritemerkintaDTO.lukittu = false
        val user = userService.getAuthenticatedUser(principal)

        suoritemerkintaService.save(suoritemerkintaDTO, user.id!!)?.let {
            return ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        suoritemerkintaDTO.id.toString()
                    )
                )
                .body(it)
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
        log.debug("REST request to get Suoritemerkinta : $id")

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
        log.debug("REST request to delete Suoritemerkinta : $id")

        val user = userService.getAuthenticatedUser(principal)
        suoritemerkintaService.delete(id, user.id!!)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }

    @GetMapping("/oppimistavoitteet-taulukko")
    fun getOppimistavoitteetTable(
        principal: Principal?
    ): ResponseEntity<OppimistavoitteetTableDTO> {
        log.debug("REST request to get OppimistavoitteetTable")

        val user = userService.getAuthenticatedUser(principal)
        val table = OppimistavoitteetTableDTO()

        table.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        table.suoritemerkinnat = suoritemerkintaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        return ResponseEntity.ok(table)
    }

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaForm(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        log.debug("REST request to get SuoritemerkintaForm")

        val user = userService.getAuthenticatedUser(principal)
        val form = SuoritemerkintaFormDTO()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        form.kunnat = kuntaService.findAll().toMutableSet()
        form.erikoisalat = erikoisalaService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        form.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        return ResponseEntity.ok(form)
    }
}
