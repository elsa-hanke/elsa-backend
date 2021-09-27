package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.tyoskentelyaikaErrorKey
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val TYOSKENTELYJAKSO_ENTITY_NAME = "tyoskentelyjakso"
private const val KESKEYTYSAIKA_ENTITY_NAME = "keskeytysaika"
private const val ASIAKIRJA_ENTITY_NAME = "asiakirja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTyoskentelyjaksoResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val poissaolonSyyService: PoissaolonSyyService,
    private val keskeytysaikaService: KeskeytysaikaService,
    private val asiakirjaService: AsiakirjaService,
    private val objectMapper: ObjectMapper,
    private val fileValidationService: FileValidationService,
    private val overlappingTyoskentelyjaksoValidationService: OverlappingTyoskentelyjaksoValidationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            validateNewTyoskentelyjaksoDTO(it)
            validatePaattymispaiva(user.id!!, it)
            validateTyoskentelyaika(user.id!!, it)

            val asiakirjaDTOs = getMappedFiles(files, user.id!!) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, user.id!!, asiakirjaDTOs)?.let { result ->
                return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .headers(
                        HeaderUtil.createEntityCreationAlert(
                            applicationName,
                            true,
                            TYOSKENTELYJAKSO_ENTITY_NAME,
                            result.id.toString()
                        )
                    )
                    .body(result)
            }

        } ?: throw BadRequestAlertException(
            "Työskentelyjakson lisääminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal"
        )
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            if (it.id == null) {
                throw BadRequestAlertException(
                    "Työskentelyjakson ID puuttuu.",
                    TYOSKENTELYJAKSO_ENTITY_NAME,
                    "idnull"
                )
            }
            validatePaattymispaiva(user.id!!, it)
            validateTyoskentelyaika(user.id!!, it)

            val newAsiakirjat = getMappedFiles(files, user.id!!) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            tyoskentelyjaksoService.update(it, user.id!!, newAsiakirjat, deletedAsiakirjaIds)
                ?.let { result ->
                    return ResponseEntity.ok()
                        .headers(
                            HeaderUtil.createEntityUpdateAlert(
                                applicationName,
                                true,
                                TYOSKENTELYJAKSO_ENTITY_NAME,
                                result.id.toString()
                            )
                        )
                        .body(result)
                }
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        log.debug("REST request to get a page of Tyoskentelyjakso")

        val user = userService.getAuthenticatedUser(principal)
        val table = TyoskentelyjaksotTableDTO()
        table.poissaolonSyyt = poissaolonSyyService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(user.id!!)

        return ResponseEntity.ok(table)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getTyoskentelyjaksot(
        principal: Principal?
    ): ResponseEntity<List<TyoskentelyjaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!)

        return ResponseEntity.ok(tyoskentelyjaksot)
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to get Tyoskentelyjakso : $id")

        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        log.debug("REST request to delete Tyoskentelyjakso : $id")

        val user = userService.getAuthenticatedUser(principal)

        asiakirjaService.removeTyoskentelyjaksoReference(user.id!!, id)
        tyoskentelyjaksoService.delete(id, user.id!!)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    TYOSKENTELYJAKSO_ENTITY_NAME,
                    id.toString()
                )
            ).build()
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        log.debug("REST request to get TyoskentelyjaksoForm")

        val user = userService.getAuthenticatedUser(principal)
        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByErikoistuvaLaakariUserId(user.id!!).map { it.nimi!! }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        log.debug("REST request to get PoissaoloForm")

        val user = userService.getAuthenticatedUser(principal)

        val form = KeskeytysaikaFormDTO()

        form.poissaolonSyyt = poissaolonSyyService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/tyoskentelyjaksot/poissaolot")
    fun createKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        log.debug("REST request to create Keskeytysaika : $keskeytysaikaDTO")

        if (keskeytysaikaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi keskeytysaika ei saa sisältää ID:tä.",
                "keskeytysaika",
                "idexists"
            )
        }
        if (keskeytysaikaDTO.alkamispaiva!!.isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                "keskeytysaika",
                "dataillegal"
            )
        }

        val user = userService.getAuthenticatedUser(principal)

        keskeytysaikaService.save(keskeytysaikaDTO, user.id!!)?.let {
            return ResponseEntity.created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        "keskeytysaika",
                        it.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan lisääminen epäonnistui.",
            "keskeytysaika",
            "dataillegal"
        )
    }

    @PutMapping("/tyoskentelyjaksot/poissaolot")
    fun updateKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        log.debug("REST request to update Keskeytysaika : $keskeytysaikaDTO")

        if (keskeytysaikaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", TYOSKENTELYJAKSO_ENTITY_NAME, "idnull")
        }

        val user = userService.getAuthenticatedUser(principal)
        validateKeskeytysaika(user.id!!, keskeytysaikaDTO)
        keskeytysaikaService.save(keskeytysaikaDTO, user.id!!)?.let {
            return ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        "keskeytysaika",
                        keskeytysaikaDTO.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan päivittäminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        log.debug("REST request to get Keskeytysaika : $id")

        val user = userService.getAuthenticatedUser(principal)
        keskeytysaikaService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        log.debug("REST request to delete Keskeytysaika : $id")

        val user = userService.getAuthenticatedUser(principal)
        validateKeskeytysaikaDelete(user.id!!, id)
        keskeytysaikaService.delete(id, user.id!!)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    "keskeytysaika",
                    id.toString()
                )
            )
            .build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    fun updateLiitettyKoejaksoon(
        @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO?> {
        val user = userService.getAuthenticatedUser(principal)
        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", TYOSKENTELYJAKSO_ENTITY_NAME, "idnull")
        }

        if (tyoskentelyjaksoDTO.liitettyKoejaksoon == null) {
            throw BadRequestAlertException(
                "liitettyKoejaksoon on pakollinen tieto",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "illegaldata"
            )
        }

        tyoskentelyjaksoService.updateLiitettyKoejaksoon(
            tyoskentelyjaksoDTO.id!!,
            user.id!!,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        "tyoskentelyjakso",
                        it.id.toString()
                    )
                )
            return if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal"
        )
    }

    private fun getMappedFiles(
        files: List<MultipartFile>?,
        userId: String
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            fileValidationService.validate(it, userId)

            if (!fileValidationService.validate(it, userId)) {
            throw BadRequestAlertException(
                "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                ASIAKIRJA_ENTITY_NAME,
                "illegaldata"
            )
        }

            return it.map { file ->
                AsiakirjaDTO(
                    nimi = file.originalFilename,
                    tyyppi = file.contentType,
                    asiakirjaData = AsiakirjaDataDTO(
                        fileInputStream = file.inputStream,
                        fileSize = file.size
                    )
                )
            }.toMutableSet()
        }

        return null
    }

    private fun validateNewTyoskentelyjaksoDTO(it: TyoskentelyjaksoDTO) {
        if (it.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä.",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idexists"
            )
        }
        if (it.tyoskentelypaikka == null || it.tyoskentelypaikka!!.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä.",
                "tyoskentelypaikka",
                "idexists"
            )
        }
    }

    private fun validateTyoskentelyaika(userId: String, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (!overlappingTyoskentelyjaksoValidationService.validateTyoskentelyjakso(userId, tyoskentelyjaksoDTO)) {
            throw BadRequestAlertException(
                "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                tyoskentelyaikaErrorKey
            )
        }
    }

    private fun validatePaattymispaiva(userId: String, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    "tyoskentelypaikka",
                    "dataillegal"
                )
            }
        }

        if (!tyoskentelyjaksoService.validatePaattymispaiva(tyoskentelyjaksoDTO, userId)) {
            throw BadRequestAlertException(
                "Työskentelyjakson päättymispäivä ei ole kelvollinen.",
                "tyoskentelyjakso",
                "dataillegal"
            )
        }
    }

    private fun validateKeskeytysaika(
        userId: String,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ) {
        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaika(
                userId,
                keskeytysaikaDTO
            )
        ) {
            throw BadRequestAlertException(
                "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                tyoskentelyaikaErrorKey
            )
        }
    }

    private fun validateKeskeytysaikaDelete(
        userId: String,
        keskeytysaikaId: Long
    ) {
        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaikaDelete(userId, keskeytysaikaId)) {
            throw BadRequestAlertException(
                "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                tyoskentelyaikaErrorKey
            )
        }
    }
}
