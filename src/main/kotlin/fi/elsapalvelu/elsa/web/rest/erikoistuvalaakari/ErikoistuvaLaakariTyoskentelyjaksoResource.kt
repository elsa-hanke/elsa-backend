package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.required

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
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonTarkistuspalvelu
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
    private val tyoskentelyjaksonMuokkauspalvelu: TyoskentelyjaksonMuokkauspalvelu,
    private val keskeytysajanMuokkauspalvelu: KeskeytysajanMuokkauspalvelu,
    private val opintooikeusService: OpintooikeusService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val opintosuoritusService: OpintosuoritusService,
    private val tyoskentelyjaksonTarkistuspalvelu: TyoskentelyjaksonTarkistuspalvelu
) {

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @RequestParam files: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

        return tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            val opintooikeusId =
                opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
            val tulos = tyoskentelyjaksonMuokkauspalvelu.luo(it, opintooikeusId, files)
            ResponseEntity.created(URI("/api/tyoskentelyjaksot/${tulos.id}")).body(tulos)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())

        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

        return tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            ResponseEntity.ok(tyoskentelyjaksonMuokkauspalvelu.paivita(it, opintooikeusId, files, deletedAsiakirjaIdsJson))
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeus =
            opintooikeusService.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        if (opintooikeus.erikoisalaId == YEK_ERIKOISALA_ID) {
            throw BadRequestAlertException(
                "Erikoistuvan työskentelyjaksoihin ei pääse YEK erikoisalan opinto-oikeudella",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "yekopintooikeus"
            )
        }
        val table = TyoskentelyjaksotTableDTO()

        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeus.id.required()).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeus.id.required()).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(opintooikeus.id.required())

        val erikoistuvaLaakariId = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id.required())?.id.required()

        // If erikoisala is hammaslääketiede, return terveyskeskuskoulutus = hyväksytty since on that specialty it is not needed to be completed
        if (opintooikeus.erikoisalaId != null && opintooikeus.erikoisalaId.required()::class.simpleName == "Long") {
            val erikoisAla: ErikoisalaDTO? = erikoisalaService.findOne(opintooikeus.erikoisalaId.required()).getOrNull()
            val hammaslaaketiede: ErikoisalaTyyppi = enumValueOf("HAMMASLAAKETIEDE")
            if (erikoisAla != null && erikoisAla.tyyppi == hammaslaaketiede) {
                table.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
                table.terveyskeskuskoulutusjaksonHyvaksymispvm = null
                table.terveyskeskuskoulutusjaksonKorjausehdotus = hammaslaaketiede.toString()

                return ResponseEntity.ok(table)
            }
        }

        val terveyskeskusSuoritus = opintosuoritusService.getTerveyskoulutusjaksoSuoritusPvm(opintooikeus.id.required(), erikoistuvaLaakariId)
        if (terveyskeskusSuoritus != null) {
            table.terveyskeskuskoulutusjaksonTila = TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY
            table.terveyskeskuskoulutusjaksonHyvaksymispvm = terveyskeskusSuoritus
        } else {
            val hyvaksynta = terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusId(opintooikeus.id.required())
            if (hyvaksynta != null) {
                table.terveyskeskuskoulutusjaksonTila = hyvaksynta.tila
                table.terveyskeskuskoulutusjaksonKorjausehdotus =
                    if (hyvaksynta.virkailijanKorjausehdotus != null) hyvaksynta.virkailijanKorjausehdotus else hyvaksynta.vastuuhenkilonKorjausehdotus
                table.terveyskeskuskoulutusjaksonHyvaksymispvm = hyvaksynta.vastuuhenkilonKuittausaika
            } else if (terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(opintooikeus.id.required())) {
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        return tyoskentelyjaksoService.findOne(id, opintooikeusId)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("/tyoskentelyjaksot/{id}/asiakirjat")
    fun updateTyoskentelyjaksoAsiakirjat(
        @PathVariable id: Long,
        @RequestParam addedFiles: List<MultipartFile>?,
        @RequestParam deletedFiles: List<Int>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())

        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), ASIAKIRJA_ENTITY_NAME, "erikoistujan")

        return ResponseEntity.ok(tyoskentelyjaksonMuokkauspalvelu.paivitaAsiakirjat(id, opintooikeusId, addedFiles, deletedFiles))
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), TYOSKENTELYJAKSO_ENTITY_NAME, "erikoistujan")

        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())

        tyoskentelyjaksonMuokkauspalvelu.poista(id, opintooikeusId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi.required() }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
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
        keskeytysajanMuokkauspalvelu.tarkistaUudenKeskeytysajanTunniste(keskeytysaikaDTO)

        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksonTarkistuspalvelu.tarkistaKeskeytysaika(keskeytysaikaDTO)
        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        val tulos = keskeytysajanMuokkauspalvelu.luo(keskeytysaikaDTO, opintooikeusId)
        return ResponseEntity.created(URI("/api/tyoskentelyjaksot/poissaolot/${tulos.id}")).body(tulos)
    }

    @PutMapping("/tyoskentelyjaksot/poissaolot")
    fun updateKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        keskeytysajanMuokkauspalvelu.tarkistaPaivitettavanKeskeytysajanTunniste(keskeytysaikaDTO)

        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksonTarkistuspalvelu.tarkistaKeskeytysaika(keskeytysaikaDTO)
        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        return ResponseEntity.ok(keskeytysajanMuokkauspalvelu.paivita(keskeytysaikaDTO, opintooikeusId))
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
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

        tyoskentelyjaksonTarkistuspalvelu.tarkistaMuokkausoikeudet(principal, user.id.required(), KESKEYTYSAIKA_ENTITY_NAME, "erikoistujan")

        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        keskeytysajanMuokkauspalvelu.poista(id, opintooikeusId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    @PreAuthorize("!hasRole('ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA')")
    fun updateLiitettyKoejaksoon(
        @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        val tulos = tyoskentelyjaksonMuokkauspalvelu.paivitaKoejaksokytkenta(tyoskentelyjaksoDTO, opintooikeusId)
        return if (tyoskentelyjaksoDTO.liitettyKoejaksoon.required()) ResponseEntity.ok(tulos) else ResponseEntity.ok().build()
    }

    @GetMapping("/tyoskentelyjaksot/terveyskeskuskoulutusjakso")
    fun getTerveyskeskuskoulutusjakso(principal: Principal?): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())
        val erikoistuvaLaakariId = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id.required())?.id.required()

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

        tyoskentelyjaksonTarkistuspalvelu.tarkistaLaillistamispaivaJaTodistus(user, laillistamispaiva, laillistamispaivanLiite)
        if (terveyskeskuskoulutusjaksonHyvaksyntaService.existsByOpintooikeusId(opintooikeusId)) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson hyväksyntä on jo lähetetty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-hyvaksynta-on-jo-lahetetty"
            )
        }

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id.required(),
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id.required())

        val hyvaksynta =
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByOpintooikeusId(opintooikeusId)
                ?: throw BadRequestAlertException(
                    "Terveyskeskuskoulutusjakson hyväksyntää ei löydy",
                    TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                    "dataillegal.terveyskeskuskoulutusjakson-hyvaksyntaa-ei-loydy"
                )

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id.required(),
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        terveyskeskuskoulutusjaksonHyvaksyntaService.update(
            user.id.required(),
            false,
            hyvaksynta.id.required(),
            null,
            null
        )
            .let {
                return ResponseEntity.ok(it)
            }
    }


}
