package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.security.Principal
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
import fi.elsapalvelu.elsa.web.rest.ENTITY_KOEJAKSON_SOPIMUS
import fi.elsapalvelu.elsa.web.rest.common.KoejaksoResourceSupport
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil


@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksoResourceSupport: KoejaksoResourceSupport
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

        val user = userService.getAuthenticatedUser(principal)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)

        if (existingKoulutussopimusDTO.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Koulutussopimusta ei saa muokata, jos erikoistuva ei ole hyväksynyt sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.koulutussopimusta-ei-saa-muokata-jos-erikoistua-ei-ole-hyvaksynyt-sita"
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
        val aloituskeskusteluDTO = koejaksoResourceSupport.findByLahikouluttajaOrLahiesimies(
            id,
            user.id!!,
            koejaksonAloituskeskusteluService::findOneByIdAndLahikouluttajaUserId,
            koejaksonAloituskeskusteluService::findOneByIdAndLahiesimiesUserId
        )
        return ResponseUtil.wrapOrNotFound(aloituskeskusteluDTO)
    }

    @GetMapping("/koejakso/valiarviointi/{id}")
    fun getValiarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valiarviointiDTO = koejaksoResourceSupport.findByLahikouluttajaOrLahiesimies(
            id,
            user.id!!,
            koejaksonValiarviointiService::findOneByIdAndLahikouluttajaUserId,
            koejaksonValiarviointiService::findOneByIdAndLahiesimiesUserId
        )
        return ResponseUtil.wrapOrNotFound(valiarviointiDTO)
    }

    @GetMapping("/koejakso/kehittamistoimenpiteet/{id}")
    fun getKehittamistoimenpiteet(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kehittamistoimenpiteetDTO = koejaksoResourceSupport.findByLahikouluttajaOrLahiesimies(
            id,
            user.id!!,
            koejaksonKehittamistoimenpiteetService::findOneByIdAndLahikouluttajaUserId,
            koejaksonKehittamistoimenpiteetService::findOneByIdAndLahiesimiesUserId
        )
        return ResponseUtil.wrapOrNotFound(kehittamistoimenpiteetDTO)
    }

    @GetMapping("/koejakso/loppukeskustelu/{id}")
    fun getLoppukeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val loppukeskusteluDTO = koejaksoResourceSupport.findByLahikouluttajaOrLahiesimies(
            id,
            user.id!!,
            koejaksonLoppukeskusteluService::findOneByIdAndLahikouluttajaUserId,
            koejaksonLoppukeskusteluService::findOneByIdAndLahiesimiesUserId
        )
        return ResponseUtil.wrapOrNotFound(loppukeskusteluDTO)
    }
}
