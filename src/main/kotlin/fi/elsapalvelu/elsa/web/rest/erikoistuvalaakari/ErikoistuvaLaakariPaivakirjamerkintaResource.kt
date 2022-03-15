package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.PaivakirjamerkintaCriteria
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkinnatOptionsDTO
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import tech.jhipster.service.filter.BooleanFilter
import java.net.URI
import java.security.Principal
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariPaivakirjamerkintaResource(
    private val userService: UserService,
    private val paivakirjamerkintaService: PaivakirjamerkintaService,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaQueryService: PaivakirjamerkintaQueryService,
    private val teoriakoulutusService: TeoriakoulutusService,
    private val paivakirjaAihekategoriaService: PaivakirjaAihekategoriaService,
    private val opintooikeusService: OpintooikeusService
) {

    companion object {
        const val ENTITY_NAME = "paivakirjamerkinta"
    }

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/paivakirjamerkinnat")
    fun createPaivakirjamerkinta(
        @Valid @RequestBody paivakirjamerkintaDTO: PaivakirjamerkintaDTO,
        principal: Principal?
    ): ResponseEntity<PaivakirjamerkintaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (paivakirjamerkintaDTO.id != null) {
            throw BadRequestAlertException(
                "A new paivakirjamerkinta cannot already have an ID",
                ENTITY_NAME,
                "idexists"
            )
        }

        return paivakirjamerkintaService.save(paivakirjamerkintaDTO, opintooikeusId)?.let {
            ResponseEntity
                .created(URI("/api/erikoistuva-laakari/paivakirjamerkinnat/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/paivakirjamerkinnat/{id}")
    fun updatePaivakirjamerkinta(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid @RequestBody paivakirjamerkintaDTO: PaivakirjamerkintaDTO,
        principal: Principal?
    ): ResponseEntity<PaivakirjamerkintaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (paivakirjamerkintaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }

        if (!Objects.equals(id, paivakirjamerkintaDTO.id)) {
            throw BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid")
        }

        if (!paivakirjamerkintaRepository.existsById(id)) {
            throw BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound")
        }

        val result = paivakirjamerkintaService.save(paivakirjamerkintaDTO, opintooikeusId)

        return ResponseEntity.ok(result)
    }

    @GetMapping("/paivakirjamerkinnat")
    fun getAllPaivakirjamerkinnat(
        criteria: PaivakirjamerkintaCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<PaivakirjamerkintaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if ((principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
                .contains(ERIKOISTUVA_LAAKARI_IMPERSONATED)
        ) {
            criteria.yksityinen = BooleanFilter()
            criteria.yksityinen?.equals = false
        }

        return ResponseEntity.ok(
            paivakirjamerkintaQueryService.findByCriteriaAndOpintooikeusId(
                criteria,
                pageable,
                opintooikeusId
            )
        )
    }

    @GetMapping("/paivakirjamerkinnat/{id}")
    fun getPaivakirjamerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<PaivakirjamerkintaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        return paivakirjamerkintaService.findOne(id, opintooikeusId)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/paivakirjamerkinnat/{id}")
    fun deletePaivakirjamerkinta(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        paivakirjamerkintaService.delete(id, opintooikeusId)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/paivakirjamerkinta-lomake")
    fun getPaivakirjamerkintaForm(
        principal: Principal?
    ): ResponseEntity<PaivakirjamerkintaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = PaivakirjamerkintaFormDTO()
        form.aihekategoriat = paivakirjaAihekategoriaService.findAll().toMutableSet()
        form.teoriakoulutukset = teoriakoulutusService.findAll(opintooikeusId).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/paivakirjamerkinnat-rajaimet")
    fun getPaivakirjamerkinnatRajaimet(): ResponseEntity<PaivakirjamerkinnatOptionsDTO> {
        val form = PaivakirjamerkinnatOptionsDTO()
        form.aihekategoriat = paivakirjaAihekategoriaService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }
}
