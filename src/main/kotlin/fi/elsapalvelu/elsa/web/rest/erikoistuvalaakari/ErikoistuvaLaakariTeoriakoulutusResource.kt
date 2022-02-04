package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari


import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutuksetDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
    private val fileValidationService: FileValidationService,
    private val objectMapper: ObjectMapper,
    private val opintoopasService: OpintoopasService,
    private val opintooikeusService: OpintooikeusService
) {

    companion object {
        const val ENTITY_NAME = "teoriakoulutus"
        const val ASIAKIRJA_ENTITY_NAME = "asiakirja"
    }

    @PostMapping("/teoriakoulutukset")
    fun createTeoriakoulutus(
        @Valid teoriakoulutusDTO: TeoriakoulutusDTO,
        @RequestParam todistusFiles: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (teoriakoulutusDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi teoriakoulutus ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }

        val todistukset = getMappedFiles(todistusFiles, opintooikeusId) ?: mutableSetOf()
        return teoriakoulutusService.save(teoriakoulutusDTO, todistukset, null, opintooikeusId)?.let {
            ResponseEntity
                .created(URI("/api/erikoistuva-laakari/teoriakoulutukset/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/teoriakoulutukset/{id}")
    fun updateTeoriakoulutus(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid teoriakoulutusDTO: TeoriakoulutusDTO,
        @RequestParam todistusFiles: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (teoriakoulutusDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }

        if (!Objects.equals(id, teoriakoulutusDTO.id)) {
            throw BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid")
        }

        if (!teoriakoulutusRepository.existsById(id)) {
            throw BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound")
        }

        val todistukset = getMappedFiles(todistusFiles, opintooikeusId) ?: mutableSetOf()
        val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let {
            objectMapper.readValue(it, mutableSetOf<Int>()::class.java)
        }
        val result = teoriakoulutusService.save(teoriakoulutusDTO, todistukset, deletedAsiakirjaIds, opintooikeusId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/teoriakoulutukset")
    fun getAllTeoriakoulutukset(
        principal: Principal?
    ): ResponseEntity<TeoriakoulutuksetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeus = opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val teoriakoulutukset = teoriakoulutusService.findAll(opintooikeus.id!!)
        val opintoopas = opintoopasService.findOne(opintooikeus.opintoopasId!!)

        return ResponseEntity.ok(
            TeoriakoulutuksetDTO(
                teoriakoulutukset = teoriakoulutukset.toMutableSet(),
                erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara
                    ?: 0.0
            )
        )
    }

    @GetMapping("/teoriakoulutukset/{id}")
    fun getTeoriakoulutus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TeoriakoulutusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        return teoriakoulutusService.findOne(id, opintooikeusId)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/teoriakoulutukset/{id}")
    fun deleteTeoriakoulutus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        teoriakoulutusService.delete(id, opintooikeusId)
        return ResponseEntity.noContent().build()
    }

    private fun getMappedFiles(
        files: List<MultipartFile>?,
        opintooikeusId: Long
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            if (!fileValidationService.validate(it, opintooikeusId)) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ASIAKIRJA_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.map { file -> file.mapAsiakirja() }.toMutableSet()
        }

        return null
    }
}
