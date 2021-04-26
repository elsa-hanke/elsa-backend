package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.KoejaksonAloituskeskusteluService
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
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

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejakso")
    fun getKoejakso(
        principal: Principal?
    ): ResponseEntity<KoejaksoDTO> {
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

        return ResponseEntity.ok(result)
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
        if (aloituskeskusteluDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi Aloituskeskustelu ei saa sisältää ID:tä.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "idexists"
            )
        }
        validateAloituskeskustelu(aloituskeskusteluDTO)

        val aloituskeskustelu =
            koejaksonAloituskeskusteluService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (aloituskeskustelu.isPresent) {
            throw BadRequestAlertException(
                "Käyttäjällä on jo koejakson aloituskeskustelu.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "entityexists"
            )
        }

        val result =
            koejaksonAloituskeskusteluService.create(aloituskeskusteluDTO, user.id!!)
        return ResponseEntity.created(URI("/api/koejakso/aloituskeskustelu/${result.id}"))
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

    @PutMapping("/koejakso/aloituskeskustelu")
    fun updateAloituskeskustelu(
        @Valid @RequestBody aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        if (aloituskeskusteluDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "idnull"
            )
        }

        validateAloituskeskustelu(aloituskeskusteluDTO)

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
                "Lähetettyä aloituskeskustelua ei saa muokata.",
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

    private fun validateKoulutussopimus(koulutussopimusDTO: KoejaksonKoulutussopimusDTO) {
        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
        if (koulutussopimusDTO.kouluttajat?.any { k -> k.sopimusHyvaksytty == true || k.kuittausaika != null }!!) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää kouluttajien kuittausta. Kouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
    }

    private fun validateAloituskeskustelu(aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO) {
        if (aloituskeskusteluDTO.lahikouluttaja?.sopimusHyvaksytty == true || aloituskeskusteluDTO.lahikouluttaja?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Koejakson aloituskeskustelu ei saa sisältää lähikouluttaja kuittausta. Lähikouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }
        if (aloituskeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true || aloituskeskusteluDTO.lahiesimies?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Koejakson aloituskeskustelu ei saa sisältää lähiesimiehen kuittausta. Lähiesimies määrittelee sen.",
                ENTITY_KOEJAKSON_ALOITUSKESKUSTELU,
                "dataillegal"
            )
        }
    }
}
