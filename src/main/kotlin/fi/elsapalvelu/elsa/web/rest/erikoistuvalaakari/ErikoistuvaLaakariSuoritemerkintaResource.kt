package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.OppimistavoitteenKategoriaService
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteetTableDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "suoritemerkinta"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritemerkintaResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val oppimistavoitteenKategoriaService: OppimistavoitteenKategoriaService,
    private val suoritemerkintaService: SuoritemerkintaService,
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
                "A new suoritemerkinta cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }
        suoritemerkintaDTO.lukittu = false
        // Todo: vain omiin työskentelyjaksoihin voi lisätä suoritemerkintä
        val user = userService.getAuthenticatedUser(principal)
        val result = suoritemerkintaService.save(suoritemerkintaDTO)
        return ResponseEntity.created(URI("/api/suoritemerkinnat/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                ENTITY_NAME,
                result.id.toString()
            ))
            .body(result)
    }

    @PutMapping("/suoritemerkinnat")
    fun updateSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        if (suoritemerkintaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        suoritemerkintaDTO.lukittu = false
        // Todo: vain omiin työskentelyjaksoihin voi lisätä suoritemerkintä
        val user = userService.getAuthenticatedUser(principal)
        val result = suoritemerkintaService.save(suoritemerkintaDTO)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    suoritemerkintaDTO.id.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/suoritemerkinnat/{id}")
    fun getSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        // Todo: vain omiin työskentelyjaksoihin voi lisätä suoritemerkintä
        val user = userService.getAuthenticatedUser(principal)
        val suoritemerkintaDTO = suoritemerkintaService.findOne(id)
        return ResponseUtil.wrapOrNotFound(suoritemerkintaDTO)
    }

    @DeleteMapping("/suoritemerkinnat/{id}")
    fun deleteSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        // Todo: vain omiin työskentelyjaksoihin voi lisätä suoritemerkintä
        val user = userService.getAuthenticatedUser(principal)
        suoritemerkintaService.delete(id)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }

    @GetMapping("/oppimistavoitteet-taulukko")
    fun getOppimistavoitteetTable(
        principal: Principal?
    ): ResponseEntity<OppimistavoitteetTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!

        val table = OppimistavoitteetTableDTO()
        table.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService.findAll().toMutableSet()
        table.suoritemerkinnat = suoritemerkintaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id).toMutableSet();

        return ResponseEntity.ok(table)
    }

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaForm(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!

        val form = SuoritemerkintaFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        form.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }
}
