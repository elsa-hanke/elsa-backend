package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import jakarta.validation.Valid
import org.springframework.web.multipart.MultipartFile

private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"
private const val ENTITY_KOEJAKSON_ALOITUSKESKUSTELU = "koejakson_aloituskeskustelu"
private const val ENTITY_KOEJAKSON_VALIARVIOINTI = "koejakson_valiarviointi"
private const val ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET = "koejakson_kehittamistoimenpiteet"
private const val ENTITY_KOEJAKSON_LOPPUKESKUSTELU = "koejakson_loppukeskustelu"
private const val ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO = "koejakson_vastuuhenkilon_arvio"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val kayttajaService: KayttajaService,
    private val yliopistoService: YliopistoService,
    private val opintooikeusService: OpintooikeusService,
    private val fileValidationService: FileValidationService,
    private val objectMapper: ObjectMapper
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejakso")
    fun getKoejakso(principal: Principal?): ResponseEntity<KoejaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val result = KoejaksoDTO()

        koejaksonKoulutussopimusService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.koulutussopimus = it
            }
        result.koulutusSopimuksenTila = KoejaksoTila.fromSopimus(result.koulutussopimus, false)

        koejaksonAloituskeskusteluService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.aloituskeskustelu = it
            }
        result.aloituskeskustelunTila =
            KoejaksoTila.fromAloituskeskustelu(result.aloituskeskustelu)

        koejaksonValiarviointiService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.valiarviointi = it
            }
        result.valiarvioinninTila =
            KoejaksoTila.fromValiarvointi(
                result.aloituskeskustelunTila == KoejaksoTila.HYVAKSYTTY,
                result.valiarviointi
            )

        val valiarviointiHyvaksytty = result.valiarvioinninTila == KoejaksoTila.HYVAKSYTTY

        koejaksonKehittamistoimenpiteetService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.kehittamistoimenpiteet = it
            }
        result.kehittamistoimenpiteidenTila =
            KoejaksoTila.fromKehittamistoimenpiteet(
                valiarviointiHyvaksytty && result.valiarviointi?.edistyminenTavoitteidenMukaista != true,
                result.kehittamistoimenpiteet
            )

        koejaksonLoppukeskusteluService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.loppukeskustelu = it
            }
        result.loppukeskustelunTila =
            KoejaksoTila.fromLoppukeskustelu(
                result.kehittamistoimenpiteidenTila == KoejaksoTila.HYVAKSYTTY
                    || (valiarviointiHyvaksytty && result.valiarviointi?.edistyminenTavoitteidenMukaista == true),
                result.loppukeskustelu
            )

        koejaksonVastuuhenkilonArvioService.findByOpintooikeusId(opintooikeusId)
            .ifPresent {
                result.vastuuhenkilonArvio = it
            }
        result.vastuuhenkilonArvionTila =
            KoejaksoTila.fromVastuuhenkilonArvio(
                result.loppukeskustelunTila == KoejaksoTila.HYVAKSYTTY,
                result.vastuuhenkilonArvio
            )

        result.kunnat = kuntaService.findAll()
        result.erikoisalat = erikoisalaService.findAll()
        result.tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(result)
    }

    @GetMapping("/koulutussopimus-lomake")
    fun getKoulutussopimusForm(principal: Principal?): ResponseEntity<KoulutussopimusFormDTO> {
        val form = KoulutussopimusFormDTO().apply {
            val user = userService.getAuthenticatedUser(principal)
            vastuuhenkilo = kayttajaService.findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
                user.id!!,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
            yliopistot = yliopistoService.findAll()
        }

        return ResponseEntity.ok(form)
    }

    @PostMapping("/koejakso/koulutussopimus")
    fun createKoulutussopimus(
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (koulutussopimusDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää ID:tä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "idexists"
            )
        }
        validateKoulutussopimus(koulutussopimusDTO)

        val koulutussopimus =
            koejaksonKoulutussopimusService.findByOpintooikeusId(opintooikeusId)

        if (koulutussopimus.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koulutussopimus.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "entityexists"
            )
        }

        koejaksonKoulutussopimusService.create(koulutussopimusDTO, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/koejakso/koulutussopimus/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/koejakso/koulutussopimus")
    fun updateKoulutussopimus(
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        if (koulutussopimusDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_KOEJAKSON_SOPIMUS, "idnull")
        }

        validateKoulutussopimus(koulutussopimusDTO)

        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val koulutussopimus =
            koejaksonKoulutussopimusService.findByOpintooikeusId(opintooikeusId)

        if (!koulutussopimus.isPresent) {
            throw BadRequestAlertException(
                "Koulutussopimusta ei löydy.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimusta-ei-loydy"
            )
        }

        if (koulutussopimus.get().lahetetty == true) {
            throw BadRequestAlertException(
                "Lähetettyä koulutussopimusta ei saa muokata.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.lahetettya-koulutussopimusta-ei-saa-muokata"
            )
        }

        val result = koejaksonKoulutussopimusService.update(koulutussopimusDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/koejakso/aloituskeskustelu")
    fun createAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByOpintooikeusId(opintooikeusId)

        if (aloituskeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson aloituskeskustelu.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "entityexists"
            )
        }

        validateArviointi(
            aloituskeskusteluDTO.id,
            aloituskeskusteluDTO.lahikouluttaja,
            aloituskeskusteluDTO.lahiesimies,
            ENTITY_KOEJAKSON_ALOITUSKESKUSTELU
        )

        koejaksonAloituskeskusteluService.create(aloituskeskusteluDTO, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/koejakso/aloituskeskustelu/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByOpintooikeusId(opintooikeusId)

        if (!aloituskeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Koejakson aloituskeskustelua ei löydy.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal.koejakson-aloituskeskustelua-ei-loydy"
            )
        }

        if (aloituskeskustelu.get().lahetetty == true) {
            throw BadRequestAlertException(
                "Allekirjoitettua arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal.allekirjoitettua-arviointia-ei-saa-muokata"
            )
        }

        if (aloituskeskusteluDTO.lahikouluttaja?.sopimusHyvaksytty == true
            || aloituskeskusteluDTO.lahikouluttaja?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähikouluttaja kuittausta. " +
                    "Lähikouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal.erikoistuvan-koejakson-arviointi-ei-saa-sisaltaa-lahikouluttaja-kuittausta"
            )
        }
        if (aloituskeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true
            || aloituskeskusteluDTO.lahiesimies?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähiesimiehen kuittausta. " +
                    "Lähiesimies määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal.erikoistuvan-koejakson-arviointi-ei-saa-sisaltaa-lahiesimiehen-kuisttausta"
            )
        }

        val result = koejaksonAloituskeskusteluService.update(aloituskeskusteluDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/koejakso/aloituskeskustelu/{id}")
    fun deleteAloituskeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        koejaksonAloituskeskusteluService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }

    @PostMapping("/koejakso/valiarviointi")
    fun createValiarviointi(
        @Valid @RequestBody valiarviointiDTO: KoejaksonValiarviointiDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val valiarviointi =
            koejaksonValiarviointiService.findByOpintooikeusId(opintooikeusId)

        if (valiarviointi.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson väliarviointi.",
                ENTITY_KOEJAKSON_VALIARVIOINTI,
                "entityexists"
            )
        }

        validateArviointi(
            valiarviointiDTO.id,
            valiarviointiDTO.lahikouluttaja,
            valiarviointiDTO.lahiesimies,
            ENTITY_KOEJAKSON_VALIARVIOINTI
        )

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByOpintooikeusId(opintooikeusId)
        if (!aloituskeskustelu.isPresent || aloituskeskustelu.get().lahiesimies?.sopimusHyvaksytty != true) {
            throw BadRequestAlertException(
                "Aloituskeskustelu täytyy hyväksyä ennen väliarviointia.",
                ENTITY_KOEJAKSON_VALIARVIOINTI,
                "dataillegal.aloituskeskustelu-taytyy-hyvaksya-ennen-valiarviointia"
            )
        }

        koejaksonValiarviointiService.create(valiarviointiDTO, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/koejakso/valiarviointi/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @DeleteMapping("/koejakso/valiarviointi/{id}")
    fun deleteValiarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        koejaksonValiarviointiService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }

    @PostMapping("/koejakso/kehittamistoimenpiteet")
    fun createKehittamistoimenpiteet(
        @Valid @RequestBody kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByOpintooikeusId(opintooikeusId)

        if (kehittamistoimenpiteet.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson kehittämistoimenpiteet.",
                ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                "entityexists"
            )
        }

        validateArviointi(
            kehittamistoimenpiteetDTO.id,
            kehittamistoimenpiteetDTO.lahikouluttaja,
            kehittamistoimenpiteetDTO.lahiesimies,
            ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET
        )

        val valiarviointi =
            koejaksonValiarviointiService.findByOpintooikeusId(opintooikeusId)
        if (!valiarviointi.isPresent || valiarviointi.get().lahiesimies?.sopimusHyvaksytty != true
            || valiarviointi.get().edistyminenTavoitteidenMukaista == true
        ) {
            throw BadRequestAlertException(
                "Väliarviointi täytyy hyväksyä kehitettävillä asioilla ennen kehittämistoimenpiteitä.",
                ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                "dataillegal.valiarviointi-taytyy-hyvaksya-kehitettavilla-asioilla-ennen-kehittamistoimenpiteita"
            )
        }

        koejaksonKehittamistoimenpiteetService.create(kehittamistoimenpiteetDTO, opintooikeusId)
            ?.let {
                return ResponseEntity
                    .created(URI("/api/koejakso/kehittamistoimenpiteet/${it.id}"))
                    .body(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/koejakso/loppukeskustelu")
    fun createLoppukeskustelu(
        @Valid @RequestBody loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByOpintooikeusId(opintooikeusId)

        if (loppukeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson loppukeskustelu.",
                ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                "entityexists"
            )
        }

        validateArviointi(
            loppukeskusteluDTO.id,
            loppukeskusteluDTO.lahikouluttaja,
            loppukeskusteluDTO.lahiesimies,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val valiarviointi =
            koejaksonValiarviointiService.findByOpintooikeusId(opintooikeusId)
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByOpintooikeusId(opintooikeusId)
        val validValiarviointi =
            valiarviointi.isPresent && valiarviointi.get().lahiesimies?.sopimusHyvaksytty == true
                && valiarviointi.get().edistyminenTavoitteidenMukaista == true
        val validKehittamistoimenpiteet =
            kehittamistoimenpiteet.isPresent && kehittamistoimenpiteet.get().lahiesimies?.sopimusHyvaksytty == true
        if (!validValiarviointi && !validKehittamistoimenpiteet) {
            throw BadRequestAlertException(
                "Väliarviointi täytyy hyväksyä ilman kehitettäviä asioita " +
                    "tai kehittämistoimenpiteet täytyy hyväksyä ennen loppukeskustelua.",
                ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                "dataillegal.valiarviointi-taytyy-hyvaksya-ilman-kehitettavia-asioita-tai-kehittamistoimenpiteet-taytyy-hyvaksya-ennen-loppukeskustelua"
            )
        }

        koejaksonLoppukeskusteluService.create(loppukeskusteluDTO, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/koejakso/loppukeskustelu/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @DeleteMapping("/koejakso/loppukeskustelu/{id}")
    fun deleteLoppukeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        koejaksonLoppukeskusteluService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/vastuuhenkilonarvio-lomake")
    fun getVastuuhenkilonArvioForm(principal: Principal?): ResponseEntity<VastuuhenkilonArvioFormDTO> {
        val form = VastuuhenkilonArvioFormDTO().apply {
            val user = userService.getAuthenticatedUser(principal)
            val opintooikeusId =
                opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
            vastuuhenkilo = kayttajaService.findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
                user.id!!,
                VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN
            )
            val (tyoskentelyJaksoLiitetty, tyoskentelyjaksonPituusRiittava, tyotodistusLiitetty) =
                tyoskentelyjaksoService.validateByLiitettyKoejaksoon(opintooikeusId)
            this.tyoskentelyjaksoLiitetty = tyoskentelyJaksoLiitetty
            this.tyoskentelyjaksonPituusRiittava = tyoskentelyjaksonPituusRiittava
            this.tyotodistusLiitetty = tyotodistusLiitetty
            this.muutOpintooikeudet =
                opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(user.id!!)
                    .filter { it.id != opintooikeusId }
            val koulutussopimus =
                koejaksonKoulutussopimusService.findByOpintooikeusId(opintooikeusId)
            this.koulutussopimusHyvaksytty =
                koulutussopimus.isPresent && koulutussopimus.get().vastuuhenkilo?.sopimusHyvaksytty == true
        }

        return ResponseEntity.ok(form)
    }

    @PostMapping("/koejakso/vastuuhenkilonarvio")
    fun createVastuuhenkilonArvio(
        @Valid @RequestParam vastuuhenkilonArvioJson: String,
        @RequestParam(required = false) asiakirjaFiles: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findByOpintooikeusId(opintooikeusId)

        if (vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson vastuuhenkilön arvio.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "entityexists"
            )
        }

        vastuuhenkilonArvioJson.let {
            objectMapper.readValue(it, KoejaksonVastuuhenkilonArvioDTO::class.java)
        }?.let { vastuuhenkilonArvioDTO ->

            if (vastuuhenkilonArvioDTO.id != null) {
                throw BadRequestAlertException(
                    "Uusi vastuuhenkilön arvio ei saa sisältää ID:tä",
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    "idexists"
                )
            }

            if (vastuuhenkilonArvioDTO.vastuuhenkilo?.sopimusHyvaksytty == true
                || vastuuhenkilonArvioDTO.vastuuhenkilo?.kuittausaika != null
            ) {
                throw BadRequestAlertException(
                    "Erikoistuvan koejakson vastuuhenkilön arvio ei saa sisältää vastuuhenkilön " +
                        "kuittausta. Vastuuhenkilö määrittelee sen.",
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    "dataillegal.erikoistuvan-koejakson-vastuuhenkion-arvio-ei-saa-sisaltaa-vastuuhenkilon-kuittausta"
                )
            }

            val loppukeskustelu =
                koejaksonLoppukeskusteluService.findByOpintooikeusId(opintooikeusId)
            if (!loppukeskustelu.isPresent || loppukeskustelu.get().lahiesimies?.sopimusHyvaksytty != true) {
                throw BadRequestAlertException(
                    "Loppukeskustelu täytyy hyväksyä ennen vastuuhenkilön arviota.",
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    "dataillegal.loppukeskustelu-taytyy-hyvaksya-ennen-vastuuhenkilon-arviota"
                )
            }

            val asiakirjat = getMappedFiles(asiakirjaFiles, opintooikeusId) ?: mutableSetOf()

            koejaksonVastuuhenkilonArvioService.create(
                vastuuhenkilonArvioDTO,
                opintooikeusId,
                asiakirjat
            )?.let {
                return ResponseEntity
                    .created(URI("/api/koejakso/vastuuhenkilonarvio/${it.id}"))
                    .body(it)
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/koejakso/vastuuhenkilonarvio")
    fun updateVastuuhenkilonArvio(
        @Valid @RequestParam vastuuhenkilonArvioJson: String,
        @RequestParam asiakirjaFiles: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findByOpintooikeusId(opintooikeusId)

        if (!vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Koejakson vastuuhenkilön arviota ei löydy.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.koejakson-vastuuhenkilon-arviota-ei-loydy"
            )
        }

        vastuuhenkilonArvioJson.let {
            objectMapper.readValue(it, KoejaksonVastuuhenkilonArvioDTO::class.java)
        }?.let { vastuuhenkilonArvioDTO ->
            if (vastuuhenkilonArvioDTO.id == null) {
                throw BadRequestAlertException(
                    "Virheellinen id",
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    "idnull"
                )
            }

            if (vastuuhenkilonArvio.get().virkailija?.sopimusHyvaksytty == true) {
                throw BadRequestAlertException(
                    "Hyväksyttyä sopimusta ei voi muokata",
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    "dataillegal.erikoistuva-ei-voi-allekirjoittaa-sopimusta-jos-esimies-ei-ole-kuitannut-sita"
                )
            }

            val asiakirjat = getMappedFiles(asiakirjaFiles, opintooikeusId) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let {
                objectMapper.readValue(it, mutableSetOf<Int>()::class.java)
            }

            val result = koejaksonVastuuhenkilonArvioService.update(
                vastuuhenkilonArvioDTO,
                user.id!!,
                asiakirjat,
                deletedAsiakirjaIds
            )
            return ResponseEntity.ok(result)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    private fun validateKoulutussopimus(koulutussopimusDTO: KoejaksonKoulutussopimusDTO) {
        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true
            || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimus-ei-saa-sisaltaa-vastuuhenkilon-kuittausta"
            )
        }
        if (koulutussopimusDTO.kouluttajat?.any { k ->
                k.sopimusHyvaksytty == true
                    || k.kuittausaika != null
            } == true) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää kouluttajan kuittausta. Kouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimus-ei-saa-sisaltaa-kouluttajan-kuittausta"
            )
        }
    }

    private fun validateArviointi(
        id: Long?,
        kouluttaja: KoejaksonKouluttajaDTO?,
        esimies: KoejaksonKouluttajaDTO?,
        entity: String
    ) {
        if (id != null) {
            throw BadRequestAlertException(
                "Uusi arviointi ei saa sisältää ID:tä",
                entity,
                "idexists"
            )
        }
        if (kouluttaja?.sopimusHyvaksytty == true || kouluttaja?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähikouluttaja kuittausta. Lähikouluttaja määrittelee sen.",
                entity,
                "dataillegal.erikoistuvan-koejakson-arviointi-ei-saa-sisaltaa-lahikouluttaja-kuittausta"
            )
        }
        if (esimies?.sopimusHyvaksytty == true || esimies?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähiesimiehen kuittausta. Lähiesimies määrittelee sen.",
                entity,
                "dataillegal.erikoistuvan-koejakson-arviointi-ei-saa-sisaltaa-lahiesimiehen-kuittausta"
            )
        }
    }

    private fun validateKuittaus(kuitattu: Boolean, entity: String) {
        if (!kuitattu) {
            throw BadRequestAlertException(
                "Erikoistuva ei voi allekirjoittaa sopimusta, jos esimies ei ole kuitannut sitä",
                entity,
                "dataillegal.erikoistuva-ei-voi-allekirjoittaa-sopimusta-jos-esimies-ei-ole-kuitannut-sita"
            )
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
                    ErikoistuvaLaakariTeoriakoulutusResource.ASIAKIRJA_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.map { file -> file.mapAsiakirja() }.toMutableSet()
        }

        return null
    }
}
