package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
import java.security.Principal

private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService
) {

    @GetMapping("/koejaksot")
    fun getKoejaksot(principal: Principal?): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val koejaksonVaiheet = koejaksonVaiheetService.findAllByKouluttajaKayttajaUserId(user.id!!)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndKouluttajaKayttajaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(koulutussopimusDTO)
    }

    @PutMapping("/koejakso/koulutussopimus")
    fun updateKoulutussopimus(
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        if (koulutussopimusDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_KOEJAKSON_SOPIMUS,
                "idnull"
            )
        }

        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true
            || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null
        ) {
            throw BadRequestAlertException(
                "Koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. " +
                    "Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimus-ei-saa-sisaltaa-vastuuhenkilon-kuittausta"
            )
        }

        val user = userService.getAuthenticatedUser(principal)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)

        if (existingKoulutussopimusDTO.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Koulutussopimusta ei saa muokata, jos erikoistuva ei ole allekirjoittanut sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimusta-ei-saa-muokata-jos-erikoistua-ei-ole-allekirjoittanut-sita"
            )
        }

        val result = koejaksonKoulutussopimusService.update(koulutussopimusDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/koejakso/aloituskeskustelu/{id}")
    fun getAloituskeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        var aloituskeskusteluDTO =
            koejaksonAloituskeskusteluService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!aloituskeskusteluDTO.isPresent) {
            aloituskeskusteluDTO =
                koejaksonAloituskeskusteluService.findOneByIdAndLahiesimiesUserId(id, user.id!!)
        }
        return ResponseUtil.wrapOrNotFound(aloituskeskusteluDTO)
    }

    @GetMapping("/koejakso/valiarviointi/{id}")
    fun getValiarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        var valiarviointiDTO =
            koejaksonValiarviointiService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!valiarviointiDTO.isPresent) {
            valiarviointiDTO =
                koejaksonValiarviointiService.findOneByIdAndLahiesimiesUserId(id, user.id!!)
        }
        return ResponseUtil.wrapOrNotFound(valiarviointiDTO)
    }

    @GetMapping("/koejakso/kehittamistoimenpiteet/{id}")
    fun getKehittamistoimenpiteet(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        var kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!kehittamistoimenpiteetDTO.isPresent) {
            kehittamistoimenpiteetDTO =
                koejaksonKehittamistoimenpiteetService.findOneByIdAndLahiesimiesUserId(
                    id,
                    user.id!!
                )
        }
        return ResponseUtil.wrapOrNotFound(kehittamistoimenpiteetDTO)
    }

    @GetMapping("/koejakso/loppukeskustelu/{id}")
    fun getLoppukeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        var loppukeskusteluDTO =
            koejaksonLoppukeskusteluService.findOneByIdAndLahikouluttajaUserId(id, user.id!!)

        if (!loppukeskusteluDTO.isPresent) {
            loppukeskusteluDTO =
                koejaksonLoppukeskusteluService.findOneByIdAndLahiesimiesUserId(
                    id,
                    user.id!!
                )
        }
        return ResponseUtil.wrapOrNotFound(loppukeskusteluDTO)
    }
}
