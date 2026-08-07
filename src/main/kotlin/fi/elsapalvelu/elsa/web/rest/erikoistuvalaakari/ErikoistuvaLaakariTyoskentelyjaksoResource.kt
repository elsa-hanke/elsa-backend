package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.web.rest.TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.common.TyoskentelyjaksoResourceSupport
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import kotlin.jvm.optionals.getOrNull

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
    private val overlappingTyoskentelyjaksoValidationService: OverlappingTyoskentelyjaksoValidationService,
    private val overlappingKeskeytysaikaValidationService: OverlappingKeskeytysaikaValidationService,
    private val opintooikeusService: OpintooikeusService,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val opintosuoritusService: OpintosuoritusService,
    private val tyoskentelyjaksoResourceSupport: TyoskentelyjaksoResourceSupport
) {

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

        return tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            val opintooikeusId =
                opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
            tyoskentelyjaksoResourceSupport.validateNewTyoskentelyjaksoDTO(it)
            tyoskentelyjaksoResourceSupport.validateAlkamisJaPaattymispaiva(opintooikeusId, it)
            tyoskentelyjaksoResourceSupport.validateTyoskentelyaika(opintooikeusId, it)

            val asiakirjaDTOs = tyoskentelyjaksoResourceSupport.getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, opintooikeusId, asiakirjaDTOs)?.let { result ->
                ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .body(result)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

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

        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

        return tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            if (it.id == null) {
                throw BadRequestAlertException(
                    "Työskentelyjakson ID puuttuu.",
                    TYOSKENTELYJAKSO_ENTITY_NAME,
                    "idnull"
                )
            }
            tyoskentelyjaksoResourceSupport.validateAlkamisJaPaattymispaiva(opintooikeusId, it)
            tyoskentelyjaksoResourceSupport.validateTyoskentelyaika(opintooikeusId, it)

            val newAsiakirjat = tyoskentelyjaksoResourceSupport.getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
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
                        ResponseEntity.ok(result)
                    } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
            } catch (e: ValidationException) {
                throw tyoskentelyjaksoResourceSupport.liitettyTerveyskoulutusjaksoonException(e)
            }
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeus =
            opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (opintooikeus.erikoisalaId == YEK_ERIKOISALA_ID) {
            throw BadRequestAlertException(
                "Erikoistuvan työskentelyjaksoihin ei pääse YEK erikoisalan opinto-oikeudella",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "yekopintooikeus"
            )
        }
        val table = TyoskentelyjaksotTableDTO()

        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeus.id!!).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeus.id!!).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(opintooikeus.id!!)

        val erikoistuvaLaakariId = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)?.id!!

        // If erikoisala is hammaslääketiede, return terveyskeskuskoulutus = hyväksytty since on that specialty it is not needed to be completed
        if (opintooikeus.erikoisalaId != null && opintooikeus.erikoisalaId!!::class.simpleName == "Long") {
            val erikoisAla: ErikoisalaDTO? = erikoisalaService.findOne(opintooikeus.erikoisalaId!!).getOrNull()
            val hammaslaaketiede: ErikoisalaTyyppi = enumValueOf("HAMMASLAAKETIEDE")
            if (erikoisAla != null && erikoisAla.tyyppi == hammaslaaketiede) {
                table.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
                table.terveyskeskuskoulutusjaksonHyvaksymispvm = null
                table.terveyskeskuskoulutusjaksonKorjausehdotus = hammaslaaketiede.toString()

                return ResponseEntity.ok(table)
            }
        }

        val terveyskeskusSuoritus = opintosuoritusService.getTerveyskoulutusjaksoSuoritusPvm(opintooikeus.id!!, erikoistuvaLaakariId)
        if (terveyskeskusSuoritus != null) {
            table.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
            table.terveyskeskuskoulutusjaksonHyvaksymispvm = terveyskeskusSuoritus
        } else {
            val hyvaksynta = terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusId(opintooikeus.id!!)
            if (hyvaksynta != null) {
                table.terveyskeskuskoulutusjaksonTila = hyvaksynta.tila
                table.terveyskeskuskoulutusjaksonKorjausehdotus =
                    if (hyvaksynta.virkailijanKorjausehdotus != null) hyvaksynta.virkailijanKorjausehdotus else hyvaksynta.vastuuhenkilonKorjausehdotus
                table.terveyskeskuskoulutusjaksonHyvaksymispvm = hyvaksynta.vastuuhenkilonKuittausaika
            } else if (terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(opintooikeus.id!!)) {
                table.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.UUSI
            }
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
        return tyoskentelyjaksoService.findOne(id, opintooikeusId)?.let {
            ResponseEntity.ok(it)
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

        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, ASIAKIRJA_ENTITY_NAME, "erikoistujan")

        return try {
            tyoskentelyjaksoService.updateAsiakirjat(
                id,
                tyoskentelyjaksoResourceSupport.getMappedFiles(addedFiles, opintooikeusId),
                deletedFiles?.toSet()
            )?.let {
                ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw tyoskentelyjaksoResourceSupport.liitettyTerveyskoulutusjaksoonException(e)
        }
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

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

        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoResourceSupport.validateKeskeytysaikaDTO(keskeytysaikaDTO)
        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

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

        return try {
            keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
                ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                    .body(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw tyoskentelyjaksoResourceSupport.liitettyTerveyskoulutusjaksoonException(e)
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

        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoResourceSupport.validateKeskeytysaikaDTO(keskeytysaikaDTO)
        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

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
            tyoskentelyjaksoResourceSupport.throwOverlappingTyoskentelyjaksotException()
        }

        return try {
            keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
                ResponseEntity.ok(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } catch (e: ValidationException) {
            throw tyoskentelyjaksoResourceSupport.liitettyTerveyskoulutusjaksoonException(e)
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
        return keskeytysaikaService.findOne(id, opintooikeusId)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoResourceSupport.validateMuokkausoikeudet(principal, user.id!!, KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaikaDelete(
                opintooikeusId,
                id
            )
        ) {
            tyoskentelyjaksoResourceSupport.throwOverlappingTyoskentelyjaksotException()
        }

        try {
            keskeytysaikaService.delete(id, opintooikeusId)
        } catch (e: ValidationException) {
            throw tyoskentelyjaksoResourceSupport.liitettyTerveyskoulutusjaksoonException(e)
        }
        return ResponseEntity
            .noContent()
            .build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    @PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
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

        return tyoskentelyjaksoService.updateLiitettyKoejaksoon(
            tyoskentelyjaksoDTO.id!!,
            opintooikeusId,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
            if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
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
    @PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
    fun createTerveyskeskuskoulutusjaksonHyvaksynta(
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoistuvaLaakariId = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)?.id!!

        if (opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(opintooikeusId, erikoistuvaLaakariId)) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksynnästä on jo opintosuoritus",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjaksolla-on-opintosuoritus"
            )
        }

        if (!terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(
                opintooikeusId
            )
        ) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntään vaadittava työskentelyaika ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-pituus-liian-pieni"
            )
        }

        tyoskentelyjaksoResourceSupport.validateLaillistamispaivaAndTodistus(user, laillistamispaiva, laillistamispaivanLiite)
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
    @PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
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


}
