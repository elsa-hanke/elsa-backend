package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import javax.validation.Valid

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
    private val yliopistoService: YliopistoService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejakso")
    fun getKoejakso(principal: Principal?): ResponseEntity<KoejaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        log.debug("REST request to get Koejakso for user: $user.id")
        val result = KoejaksoDTO()

        koejaksonKoulutussopimusService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
            .ifPresent {
                result.koulutussopimus = it
            }
        result.koulutusSopimuksenTila = KoejaksoTila.fromSopimus(result.koulutussopimus)

        koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
            .ifPresent {
                result.aloituskeskustelu = it
            }
        result.aloituskeskustelunTila =
            KoejaksoTila.fromAloituskeskustelu(result.aloituskeskustelu)

        koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
            .ifPresent {
                result.valiarviointi = it
            }
        result.valiarvioinninTila =
            KoejaksoTila.fromValiarvointi(
                result.aloituskeskustelunTila == KoejaksoTila.HYVAKSYTTY,
                result.valiarviointi
            )

        val valiarviointiHyvaksytty = result.valiarvioinninTila == KoejaksoTila.HYVAKSYTTY

        koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
            .ifPresent {
                result.kehittamistoimenpiteet = it
            }
        result.kehittamistoimenpiteidenTila =
            KoejaksoTila.fromKehittamistoimenpiteet(
                valiarviointiHyvaksytty && result.valiarviointi?.edistyminenTavoitteidenMukaista != true,
                result.kehittamistoimenpiteet
            )

        koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
            .ifPresent {
                result.loppukeskustelu = it
            }
        result.loppukeskustelunTila =
            KoejaksoTila.fromLoppukeskustelu(
                result.kehittamistoimenpiteidenTila == KoejaksoTila.HYVAKSYTTY
                    || (valiarviointiHyvaksytty && result.valiarviointi?.edistyminenTavoitteidenMukaista == true),
                result.loppukeskustelu
            )

        koejaksonVastuuhenkilonArvioService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
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
            tyoskentelyjaksoService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!)

        return ResponseEntity.ok(result)
    }

    @GetMapping("/koulutussopimus-lomake")
    fun getKoulutussopimusForm(principal: Principal?): ResponseEntity<KoulutussopimusFormDTO> {
        val form = KoulutussopimusFormDTO().apply {
            val user = userService.getAuthenticatedUser(principal)
            val kayttajaUser = kayttajaService.findByUserId(user.id!!)

            vastuuhenkilot = kayttajaService.findVastuuhenkilot().filter {
                it.yliopisto?.id == kayttajaUser.get().yliopisto?.id
            }
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
        if (koulutussopimusDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää ID:tä.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "idexists"
            )
        }
        validateKoulutussopimus(koulutussopimusDTO)

        val koulutussopimus =
            koejaksonKoulutussopimusService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (koulutussopimus.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koulutussopimus.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "entityexists"
            )
        }

        val result =
            koejaksonKoulutussopimusService.create(koulutussopimusDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/koulutussopimus/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_SOPIMUS,
                    result.id.toString()
                )
            )
            .body(result)
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

        val koulutussopimus =
            koejaksonKoulutussopimusService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!koulutussopimus.isPresent) {
            throw BadRequestAlertException(
                "Koulutussopimusta ei löydy.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        if (koulutussopimus.get().lahetetty == true) {
            throw BadRequestAlertException(
                "Lähetettyä koulutussopimusta ei saa muokata.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        val result =
            koejaksonKoulutussopimusService.update(koulutussopimusDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_SOPIMUS,
                    koulutussopimusDTO.id.toString()
                )
            )
            .body(result)
    }

    @PostMapping("/koejakso/aloituskeskustelu")
    fun createAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

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
            null,
            ENTITY_KOEJAKSON_ALOITUSKESKUSTELU
        )

        val result =
            koejaksonAloituskeskusteluService.create(aloituskeskusteluDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/aloituskeskustelu/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!aloituskeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Koejakson aloituskeskustelua ei löydy.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }

        if (aloituskeskustelu.get().lahetetty == true) {
            throw BadRequestAlertException(
                "Allekirjoitettua arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }

        if (aloituskeskusteluDTO.lahikouluttaja?.sopimusHyvaksytty == true
            || aloituskeskusteluDTO.lahikouluttaja?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähikouluttaja kuittausta. " +
                    "Lähikouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }
        if (aloituskeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true
            || aloituskeskusteluDTO.lahiesimies?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähiesimiehen kuittausta. " +
                    "Lähiesimies määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }

        val result =
            koejaksonAloituskeskusteluService.update(aloituskeskusteluDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                    aloituskeskusteluDTO.id.toString()
                )
            )
            .body(result)
    }

    @PostMapping("/koejakso/valiarviointi")
    fun createValiarviointi(
        @Valid @RequestBody valiarviointiDTO: KoejaksonValiarviointiDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

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
            valiarviointiDTO.erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_VALIARVIOINTI
        )

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!aloituskeskustelu.isPresent || aloituskeskustelu.get().lahiesimies?.sopimusHyvaksytty != true) {
            throw BadRequestAlertException(
                "Aloituskeskustelu täytyy hyväksyä ennen väliarviointia.",
                ENTITY_KOEJAKSON_VALIARVIOINTI,
                "dataillegal"
            )
        }

        val result = koejaksonValiarviointiService.create(valiarviointiDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/valiarviointi/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/koejakso/valiarviointi")
    fun updateValiarviointi(
        @Valid @RequestBody valiarviointiDTO: KoejaksonValiarviointiDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!valiarviointi.isPresent) {
            throw BadRequestAlertException(
                "Koejakson väliarviointia ei löydy.",
                ENTITY_KOEJAKSON_VALIARVIOINTI,
                "dataillegal"
            )
        }

        validateKuittaus(
            valiarviointi.get().lahiesimies?.kuittausaika != null,
            ENTITY_KOEJAKSON_VALIARVIOINTI
        )

        val result = koejaksonValiarviointiService.update(valiarviointiDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VALIARVIOINTI,
                    valiarviointiDTO.id.toString()
                )
            )
            .body(result)
    }

    @PostMapping("/koejakso/kehittamistoimenpiteet")
    fun createKehittamistoimenpiteet(
        @Valid @RequestBody kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

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
            kehittamistoimenpiteetDTO.erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET
        )

        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!valiarviointi.isPresent || valiarviointi.get().erikoistuvaAllekirjoittanut != true
            || valiarviointi.get().edistyminenTavoitteidenMukaista == true
        ) {
            throw BadRequestAlertException(
                "Väliarviointi täytyy hyväksyä kehitettävillä asioilla ennen kehittämistoimenpiteitä.",
                ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                "dataillegal"
            )
        }

        val result =
            koejaksonKehittamistoimenpiteetService.create(kehittamistoimenpiteetDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/kehittamistoimenpiteet/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/koejakso/kehittamistoimenpiteet")
    fun updateKehittamistoimenpiteet(
        @Valid @RequestBody kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!kehittamistoimenpiteet.isPresent) {
            throw BadRequestAlertException(
                "Koejakson kehittämistoimenpiteitä ei löydy.",
                ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                "dataillegal"
            )
        }

        validateKuittaus(
            kehittamistoimenpiteet.get().lahiesimies?.kuittausaika != null,
            ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET
        )

        val result =
            koejaksonKehittamistoimenpiteetService.update(kehittamistoimenpiteetDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_KEHITTAMISTOIMENPITEET,
                    kehittamistoimenpiteetDTO.id.toString()
                )
            )
            .body(result)
    }

    @PostMapping("/koejakso/loppukeskustelu")
    fun createLoppukeskustelu(
        @Valid @RequestBody loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

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
            loppukeskusteluDTO.erikoistuvaAllekirjoittanut,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val valiarviointi =
            koejaksonValiarviointiService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        val kehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        val validValiarviointi =
            valiarviointi.isPresent && valiarviointi.get().erikoistuvaAllekirjoittanut == true
                && valiarviointi.get().edistyminenTavoitteidenMukaista == true
        val validKehittamistoimenpiteet =
            kehittamistoimenpiteet.isPresent && kehittamistoimenpiteet.get().erikoistuvaAllekirjoittanut == true
        if (!validValiarviointi && !validKehittamistoimenpiteet) {
            throw BadRequestAlertException(
                "Väliarviointi täytyy hyväksyä ilman kehitettäviä asioita " +
                    "tai kehittämistoimenpiteet täytyy hyväksyä ennen loppukeskustelua.",
                ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                "dataillegal"
            )
        }

        val result =
            koejaksonLoppukeskusteluService.create(loppukeskusteluDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/loppukeskustelu/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/koejakso/loppukeskustelu")
    fun updateLoppukeskustelu(
        @Valid @RequestBody loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!loppukeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Koejakson loppukeskustelua ei löydy.",
                ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                "dataillegal"
            )
        }

        validateKuittaus(
            loppukeskustelu.get().lahiesimies?.kuittausaika != null,
            ENTITY_KOEJAKSON_LOPPUKESKUSTELU
        )

        val result = koejaksonLoppukeskusteluService.update(loppukeskusteluDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_LOPPUKESKUSTELU,
                    loppukeskusteluDTO.id.toString()
                )
            )
            .body(result)
    }

    @PostMapping("/koejakso/vastuuhenkilonarvio")
    fun createVastuuhenkilonArvio(
        @Valid @RequestBody vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson vastuuhenkilön arvio.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "entityexists"
            )
        }

        if (vastuuhenkilonArvioDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi vastuuhenkilön arvio ei saa sisältää ID:tä.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "idexists"
            )
        }

        if (vastuuhenkilonArvioDTO.vastuuhenkilo?.sopimusHyvaksytty == true
            || vastuuhenkilonArvioDTO.vastuuhenkilo?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson vastuhenkilön arvio ei saa sisältää vastuuhenkilön " +
                    "kuittausta. Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal"
            )
        }

        val loppukeskustelu =
            koejaksonLoppukeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!loppukeskustelu.isPresent || loppukeskustelu.get().erikoistuvaAllekirjoittanut != true) {
            throw BadRequestAlertException(
                "Loppukeskustelu täytyy hyväksyä ennen vastuuhenkilön arviota.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal"
            )
        }

        val result =
            koejaksonVastuuhenkilonArvioService.create(vastuuhenkilonArvioDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/vastuuhenkilonarvio/${result.id}"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    result.id.toString()
                )
            )
            .body(result)
    }

    @PutMapping("/koejakso/vastuuhenkilonarvio")
    fun updateVastuuhenkilonArvio(
        @Valid @RequestBody vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Koejakson vastuuhenkilön arviota ei löydy.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal"
            )
        }

        if (vastuuhenkilonArvioDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "idnull"
            )
        }

        validateKuittaus(
            vastuuhenkilonArvio.get().vastuuhenkilo?.kuittausaika != null,
            ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO
        )

        val result = koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    vastuuhenkilonArvioDTO.id.toString()
                )
            )
            .body(result)
    }

    private fun validateKoulutussopimus(koulutussopimusDTO: KoejaksonKoulutussopimusDTO) {
        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true
            || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
        if (koulutussopimusDTO.kouluttajat?.any { k ->
                k.sopimusHyvaksytty == true
                    || k.kuittausaika != null
            } == true) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää kouluttajan kuittausta. Kouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
    }

    private fun validateArviointi(
        id: Long?,
        kouluttaja: KoejaksonKouluttajaDTO?,
        esimies: KoejaksonKouluttajaDTO?,
        erikoistuvaAllekirjoittanut: Boolean?,
        entity: String
    ) {
        if (id != null) {
            throw BadRequestAlertException(
                "Uusi arviointi ei saa sisältää ID:tä.",
                entity,
                "idexists"
            )
        }
        if (kouluttaja?.sopimusHyvaksytty == true || kouluttaja?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähikouluttaja kuittausta. Lähikouluttaja määrittelee sen.",
                entity,
                "dataillegal"
            )
        }
        if (esimies?.sopimusHyvaksytty == true || esimies?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää lähiesimiehen kuittausta. Lähiesimies määrittelee sen.",
                entity,
                "dataillegal"
            )
        }
        if (erikoistuvaAllekirjoittanut == true) {
            throw BadRequestAlertException(
                "Erikoistuvan koejakson arviointi ei saa sisältää erikoistuvan kuittausta ennen kuin lähiesimies on hyväksynyt vaiheen.",
                entity,
                "dataillegal"
            )
        }
    }

    private fun validateKuittaus(kuitattu: Boolean, entity: String) {
        if (!kuitattu) {
            throw BadRequestAlertException(
                "Erikoistuva ei voi allekirjoittaa sopimusta, jos esimies ei ole kuitannut sitä",
                entity,
                "dataillegal"
            )
        }
    }
}
