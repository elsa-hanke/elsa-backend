package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari


import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutuksetDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
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
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTeoriakoulutusResource(
    private val teoriakoulutusService: TeoriakoulutusService,
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val userService: UserService,
    private val erikoisalaService: ErikoisalaService,
) {

    companion object {
        const val ENTITY_NAME = "teoriakoulutus"
    }

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/teoriakoulutukset")
    fun createTeoriakoulutus(
        @Valid @RequestBody teoriakoulutusDTO: TeoriakoulutusDTO,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (teoriakoulutusDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi teoriakoulutus ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }

        return teoriakoulutusService.save(teoriakoulutusDTO, user.id!!)?.let {
            ResponseEntity
                .created(URI("/api/erikoistuva-laakari/teoriakoulutukset/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/teoriakoulutukset/{id}")
    fun updateTeoriakoulutus(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid @RequestBody teoriakoulutusDTO: TeoriakoulutusDTO,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (teoriakoulutusDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }

        if (!Objects.equals(id, teoriakoulutusDTO.id)) {
            throw BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid")
        }

        if (!teoriakoulutusRepository.existsById(id)) {
            throw BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound")
        }

        val result = teoriakoulutusService.save(teoriakoulutusDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/teoriakoulutukset")
    fun getAllTeoriakoulutukset(
        principal: Principal?
    ): ResponseEntity<TeoriakoulutuksetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val teoriakoulutukset = teoriakoulutusService.findAll(user.id!!)
        val erikoisala = erikoisalaService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!)
        return ResponseEntity.ok(
            TeoriakoulutuksetDTO(
                teoriakoulutukset = teoriakoulutukset.toMutableSet(),
                erikoisala = erikoisala.firstOrNull()
            )
        )
    }

    @GetMapping("/teoriakoulutukset/{id}")
    fun getTeoriakoulutus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return teoriakoulutusService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/teoriakoulutukset/{id}")
    fun deleteTeoriakoulutus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        teoriakoulutusService.delete(id, user.id!!)
        return ResponseEntity.noContent().build()
    }
}
