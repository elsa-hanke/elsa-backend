package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.repository.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoDTO
import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoFormDTO
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

@RestController
@RequestMapping("/api/erikoistuva-laakari/koulutussuunnitelma")
class ErikoistuvaLaakariKoulutusjaksoResource(
    private val koulutusjaksoRepository: KoulutusjaksoRepository,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val userService: UserService,
    private val arvioitavanKokonaisuudenKategoriaService: ArvioitavanKokonaisuudenKategoriaService,
) {

    companion object {
        const val ENTITY_NAME = "koulutusjakso"
    }

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/koulutusjaksot")
    fun createKoulutusjakso(
        @Valid @RequestBody koulutusjaksoDTO: KoulutusjaksoDTO,
        principal: Principal?
    ): ResponseEntity<KoulutusjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (koulutusjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi koulutusjakso ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }
        koulutusjaksoService.save(koulutusjaksoDTO, user.id!!)?.let {
            return ResponseEntity
                .created(URI("/api/koulutusjaksot/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/koulutusjaksot/{id}")
    fun updateKoulutusjakso(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid @RequestBody koulutusjaksoDTO: KoulutusjaksoDTO,
        principal: Principal?
    ): ResponseEntity<KoulutusjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (koulutusjaksoDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        if (!Objects.equals(id, koulutusjaksoDTO.id)) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idinvalid")
        }

        if (!koulutusjaksoRepository.existsById(id)) {
            throw BadRequestAlertException("Entiteetti ei löydy", ENTITY_NAME, "idnotfound")
        }

        if (koulutusjaksoService.findOne(id, user.id!!)?.lukittu == true) {
            throw BadRequestAlertException(
                "Lukittua koulutusjaksoa ei voi muokata",
                ENTITY_NAME,
                "dataillegal.lukittua-koulutusjaksoa-ei-voi-muokata"
            )
        }

        val result = koulutusjaksoService.save(koulutusjaksoDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/koulutusjaksot")
    fun getAllKoulutusjaksot(
        principal: Principal?
    ): ResponseEntity<List<KoulutusjaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)

        return ResponseEntity.ok(
            koulutusjaksoService
                .findAllByKoulutussuunnitelmaErikoistuvaLaakariKayttajaUserId(user.id!!)
        )
    }

    @GetMapping("/koulutusjaksot/{id}")
    fun getKoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoulutusjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        return koulutusjaksoService.findOne(id, user.id!!)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/koulutusjaksot/{id}")
    fun deleteKoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)

        if (koulutusjaksoService.findOne(id, user.id!!)?.lukittu == true) {
            throw BadRequestAlertException(
                "Lukittua koulutusjaksoa ei voi poistaa",
                ENTITY_NAME,
                "dataillegal.lukittua-koulutusjaksoa-ei-voi-poistaa"
            )
        }

        koulutusjaksoService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/koulutusjakso-lomake")
    fun getKoulutusjaksoForm(
        principal: Principal?
    ): ResponseEntity<KoulutusjaksoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val form = KoulutusjaksoFormDTO()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        form.kunnat = kuntaService.findAll().toMutableSet()
        form.arvioitavanKokonaisuudenKategoriat = arvioitavanKokonaisuudenKategoriaService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        return ResponseEntity.ok(form)
    }
}
