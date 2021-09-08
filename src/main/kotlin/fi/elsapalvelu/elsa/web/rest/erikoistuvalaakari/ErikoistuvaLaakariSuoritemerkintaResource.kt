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
    private val kayttajaService: KayttajaService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val oppimistavoitteenKategoriaService: OppimistavoitteenKategoriaService,
    private val suoritemerkintaService: SuoritemerkintaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/suoritemerkinnat")
    fun createSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        if (suoritemerkintaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi suoritemerkinta ei saa sisältää ID:tä.",
                ENTITY_NAME,
                "idexists"
            )
        }

        // Uutta suoritemerkintää ei voi luoda lukittuna
        suoritemerkintaDTO.lukittu = false

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        suoritemerkintaService.save(suoritemerkintaDTO, kayttaja.id!!)?.let {
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
            "dataillegal"
        )
    }

    @PutMapping("/suoritemerkinnat")
    fun updateSuoritemerkinta(
        @Valid @RequestBody suoritemerkintaDTO: SuoritemerkintaDTO,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        if (suoritemerkintaDTO.id == null) {
            throw BadRequestAlertException("Epäkelvollinen id", ENTITY_NAME, "idnull")
        }
        suoritemerkintaDTO.lukittu = false
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        suoritemerkintaService.save(suoritemerkintaDTO, kayttaja.id!!)?.let {
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
            "dataillegal"
        )
    }

    @GetMapping("/suoritemerkinnat/{id}")
    fun getSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        suoritemerkintaService.findOne(id, kayttaja.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/suoritemerkinnat/{id}")
    fun deleteSuoritemerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        suoritemerkintaService.delete(id, kayttaja.id!!)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }

    @GetMapping("/oppimistavoitteet-taulukko")
    fun getOppimistavoitteetTable(
        principal: Principal?
    ): ResponseEntity<OppimistavoitteetTableDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val table = OppimistavoitteetTableDTO()

        erikoistuvaLaakariService.findOneByKayttajaId(kayttaja.id!!)?.let { kirjautunutErikoistuvaLaakari ->
            kirjautunutErikoistuvaLaakari.erikoisalaId?.let {
                table.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
                    .findAllByErikoisalaId(it).toMutableSet()
            }
        }

        table.suoritemerkinnat = suoritemerkintaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(kayttaja.id!!).toMutableSet()

        return ResponseEntity.ok(table)
    }

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaForm(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val form = SuoritemerkintaFormDTO()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaId(kayttaja.id!!).toMutableSet()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        erikoistuvaLaakariService.findOneByKayttajaId(kayttaja.id!!)?.let { kirjautunutErikoistuvaLaakari ->
            kirjautunutErikoistuvaLaakari.erikoisalaId?.let {
                form.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService
                    .findAllByErikoisalaId(it).toMutableSet()
            }
        }

        return ResponseEntity.ok(form)
    }
}
