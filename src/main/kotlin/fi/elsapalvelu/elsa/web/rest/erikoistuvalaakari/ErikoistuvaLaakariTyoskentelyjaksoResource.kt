package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import javax.persistence.EntityNotFoundException
import javax.validation.Valid
import javax.validation.ValidationException

private const val TYOSKENTELYJAKSO_ENTITY_NAME = "tyoskentelyjakso"
private const val KESKEYTYSAIKA_ENTITY_NAME = "keskeytysaika"
private const val ASIAKIRJA_ENTITY_NAME = "asiakirja"
private const val TYOSKENTELYPAIKKA_ENTITY_NAME = "tyoskentelypaikka"
private const val TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME = "terveyskeskuskoulutusjakson_hyvaksynta"

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
    private val overlappingTyoskentelyjaksoValidationService: OverlappingTyoskentelyjaksoValidationService,
    private val overlappingKeskeytysaikaValidationService: OverlappingKeskeytysaikaValidationService,
    private val opintooikeusService: OpintooikeusService,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService
) {

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
            val opintooikeusId =
                opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
            validateNewTyoskentelyjaksoDTO(it)
            validatePaattymispaiva(opintooikeusId, it)
            validateTyoskentelyaika(opintooikeusId, it)

            val asiakirjaDTOs = getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, opintooikeusId, asiakirjaDTOs)?.let { result ->
                return ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .body(result)
            }

        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
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
            validatePaattymispaiva(opintooikeusId, it)
            validateTyoskentelyaika(opintooikeusId, it)

            val newAsiakirjat = getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            try {
                tyoskentelyjaksoService.update(
                    it,
                    opintooikeusId,
                    newAsiakirjat,
                    deletedAsiakirjaIds
                )
                    ?.let { result ->
                        return ResponseEntity.ok(result)
                    }
            } catch (e: ValidationException) {
                throw liitettyTerveyskoulutusjaksoonException(e)
            }
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val table = TyoskentelyjaksotTableDTO()
        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(opintooikeusId)
        terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusId(opintooikeusId)?.let {
            table.terveyskeskuskoulutusjaksonTila = it.tila
            table.terveyskeskuskoulutusjaksonKorjausehdotus = it.korjausehdotus
            table.terveyskeskuskoulutusjaksonHyvaksymispvm = it.vastuuhenkilonKuittausaika
        }

        return ResponseEntity.ok(table)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getTyoskentelyjaksot(
        principal: Principal?
    ): ResponseEntity<List<TyoskentelyjaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(tyoskentelyjaksot)
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        tyoskentelyjaksoService.findOne(id, opintooikeusId)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("/tyoskentelyjaksot/{id}/asiakirjat")
    fun updateTyoskentelyjaksoAsiakirjat(
        @PathVariable id: Long,
        @Valid @RequestParam addedFiles: List<MultipartFile>?,
        @RequestParam deletedFiles: List<Int>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        try {
            tyoskentelyjaksoService.updateAsiakirjat(
                id,
                getMappedFiles(addedFiles, opintooikeusId),
                deletedFiles?.toSet()
            )?.let {
                return ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw liitettyTerveyskoulutusjaksoonException(e)
        }
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        asiakirjaService.removeTyoskentelyjaksoReference(id)
        koulutusjaksoService.removeTyoskentelyjaksoReference(id)
        if (tyoskentelyjaksoService.delete(id, opintooikeusId)) {
            return ResponseEntity
                .noContent()
                .build()

        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi!! }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = KeskeytysaikaFormDTO()

        form.poissaolonSyyt =
            poissaolonSyyService.findAllByOpintooikeusId(opintooikeusId).toMutableSet()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/tyoskentelyjaksot/poissaolot")
    fun createKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        if (keskeytysaikaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi keskeytysaika ei saa sisältää ID:tä",
                KESKEYTYSAIKA_ENTITY_NAME,
                "idexists"
            )
        }

        validateKeskeytysaikaDTO(keskeytysaikaDTO)
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(
                opintooikeusId,
                keskeytysaikaDTO
            )
        ) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        try {
            keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
                return ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                    .body(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw liitettyTerveyskoulutusjaksoonException(e)
        }
    }

    @PutMapping("/tyoskentelyjaksot/poissaolot")
    fun updateKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        if (keskeytysaikaDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idnull"
            )
        }

        validateKeskeytysaikaDTO(keskeytysaikaDTO)
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(
                opintooikeusId,
                keskeytysaikaDTO
            )
        ) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaika(
                opintooikeusId,
                keskeytysaikaDTO
            )
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }

        try {
            keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
                return ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw liitettyTerveyskoulutusjaksoonException(e)
        }
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        keskeytysaikaService.findOne(id, opintooikeusId)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaikaDelete(
                opintooikeusId,
                id
            )
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }

        try {
            keskeytysaikaService.delete(id, opintooikeusId)
        } catch (e: ValidationException) {
            throw liitettyTerveyskoulutusjaksoonException(e)
        }
        return ResponseEntity
            .noContent()
            .build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    fun updateLiitettyKoejaksoon(
        @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO?> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idnull"
            )
        }

        if (tyoskentelyjaksoDTO.liitettyKoejaksoon == null) {
            throw BadRequestAlertException(
                "Liitetty koejaksoon on pakollinen tieto",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.liitetty-koejaksoon-on-pakollinen-tieto"
            )
        }

        tyoskentelyjaksoService.updateLiitettyKoejaksoon(
            tyoskentelyjaksoDTO.id!!,
            opintooikeusId,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
            return if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot/terveyskeskuskoulutusjakso")
    fun getTerveyskeskuskoulutusjakso(principal: Principal?): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        try {
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusIdOrCreateNew(
                opintooikeusId
            ).let {
                return ResponseEntity.ok(it)
            }
        } catch (e: EntityNotFoundException) {
            throw BadRequestAlertException(
                "Vastuuhenkilöä ei löytynyt",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.vastuuhenkiloa-ei-loytynyt"
            )
        } catch (e: ValidationException) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson vähimmäispituus ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-vahimmaispituus-ei-tayty"
            )
        }
    }

    @PostMapping("/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta")
    fun createTerveyskeskuskoulutusjaksonHyvaksynta(
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        validateLaillistamispaivaAndTodistus(user, laillistamispaiva, laillistamispaivanLiite)
        if (terveyskeskuskoulutusjaksonHyvaksyntaService.existsByOpintooikeusId(opintooikeusId)) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntä on jo lähetetty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-hyvaksynta-on-jo-lahetetty"
            )
        }

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        terveyskeskuskoulutusjaksonHyvaksyntaService.create(opintooikeusId).let {
            return ResponseEntity.ok(it)
        }
    }

    @PutMapping("/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta")
    fun updateTerveyskeskuskoulutusjaksonHyvaksynta(
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val hyvaksynta =
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusId(opintooikeusId)
                ?: throw BadRequestAlertException(
                    "Terveyskeskuskoulutusjakson hyväksyntää ei löydy",
                    TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                    "dataillegal.terveyskeskuskoulutusjakson-hyvaksyntaa-ei-loydy"
                )

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        terveyskeskuskoulutusjaksonHyvaksyntaService.update(
            user.id!!,
            false,
            hyvaksynta.id!!,
            null,
            null
        )
            .let {
                return ResponseEntity.ok(it)
            }
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

    private fun validateLaillistamispaivaAndTodistus(
        user: UserDTO,
        laillistamispaiva: LocalDate?,
        laillistamistodistus: MultipartFile?
    ) {
        if ((laillistamispaiva == null || laillistamistodistus == null) &&
            !erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(
                user.id!!
            )
        ) {
            throw BadRequestAlertException(
                "Laillistamispaiva ja todistus vaaditaan",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.laillistamispaiva-ja-todistus-vaaditaan"
            )
        }
    }

    private fun validateNewTyoskentelyjaksoDTO(it: TyoskentelyjaksoDTO) {
        if (it.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idexists"
            )
        }
        if (it.tyoskentelypaikka == null || it.tyoskentelypaikka!!.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä",
                TYOSKENTELYPAIKKA_ENTITY_NAME,
                "idexists"
            )
        }
    }

    private fun validateTyoskentelyaika(
        opintooikeusId: Long,
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ) {
        if (!overlappingTyoskentelyjaksoValidationService.validateTyoskentelyjakso(
                opintooikeusId,
                tyoskentelyjaksoDTO
            )
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }
    }

    private fun validatePaattymispaiva(
        opintooikeusId: Long,
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    TYOSKENTELYPAIKKA_ENTITY_NAME,
                    "dataillegal.tyoskentelyjakson-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
                )
            }
        }

        if (!tyoskentelyjaksoService.validatePaattymispaiva(tyoskentelyjaksoDTO, opintooikeusId)) {
            throw BadRequestAlertException(
                "Työskentelyjakson päättymispäivä ei ole kelvollinen.",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.tyoskentelyjakson-paattymispaiva-ei-ole-kelvollinen"
            )
        }
    }

    private fun validateKeskeytysaikaDTO(keskeytysaikaDTO: KeskeytysaikaDTO) {
        if (keskeytysaikaDTO.alkamispaiva == null || keskeytysaikaDTO.paattymispaiva == null) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamis- ja päättymispäivä ovat pakollisia tietoja",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysaika-alkamis-ja-paattymispaiva-ovat-pakollisia-tietoja"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isBefore(keskeytysaikaDTO.tyoskentelyjakso!!.alkamispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamispäivä ei voi olla ennen työskentelyjakson alkamispäivää",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-alkamispaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamispaivaa"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva != null && keskeytysaikaDTO.paattymispaiva!!.isAfter(
                keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva
            )
        ) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei voi olla työskentelyjakson päättymispäivän jälkeen",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-voi-olla-tyoskentelyjakson-paattymispaivan-jalkeen"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso == null) {
            throw BadRequestAlertException(
                "Keskeytysajan täytyy kohdistua työskentelyjaksoon",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-taytyy-kohdistua-tyoskentelyjaksoon"
            )
        }
    }

    private fun throwOverlappingTyoskentelyjaksotException() {
        throw BadRequestAlertException(
            "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.paallekkaisten-tyoskentelyjaksojen-yhteenlaskettu-aika-ylittyy"
        )
    }

    private fun liitettyTerveyskoulutusjaksoonException(e: ValidationException): BadRequestAlertException {
        return BadRequestAlertException(
            e.message ?: "Validaatiovirhe",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.terveyskeskuskoulutusjaksoon-liitettya-tyoskentelyjaksoa-ei-voi-paivittaa"
        )
    }
}
