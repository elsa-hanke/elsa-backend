package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.security.Principal
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
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.jhipster.service.filter.LongFilter

@RestController
@RequestMapping("/api/vastuuhenkilo/etusivu")
class VastuuhenkiloEtusivuResource(
    private val userService: UserService,
    private val etusivuService: EtusivuService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val valmistumispyyntoService: ValmistumispyyntoService,
    private val seurantajaksoService: SeurantajaksoService,
    private val kayttajaService: KayttajaService
) {

    @GetMapping("/erikoistujien-seuranta-rajaimet")
    fun getErikoistujienSeurantaRajaimet(
        principal: Principal?
    ): ResponseEntity<ErikoistujienSeurantaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaVastuuhenkiloRajaimet(user.id.required()))
    }

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<ErikoistujanEteneminenDTO>?> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaForVastuuhenkilo(user.id.required(), criteria, pageable))
    }

    @GetMapping("/koulutettavien-seuranta")
    fun getKoulutettavienSeurantaList(
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<KoulutettavanEteneminenDTO>> {
        val userId = userService.getAuthenticatedUser(principal).id.required()
        val koulutettavat =
            etusivuService.getKoulutettavienSeurantaForVastuuhenkilo(userId, criteria, pageable)
        return ResponseEntity.ok(koulutettavat)
    }

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?
    ): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            koejaksonVaiheetService.findAllByVastuuhenkiloKayttajaUserId(
                user.id.required(),
                true
            )
        )
    }

    @GetMapping("/valmistumispyynnot")
    fun getValmistumispyynnot(
        principal: Principal?
    ): ResponseEntity<List<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            valmistumispyyntoService.findAllForVastuuhenkiloByCriteria(
                user.id.required(),
                NimiErikoisalaAndAvoinCriteria(avoin = true),
                Pageable.unpaged()
            ).content
        )
    }

    @GetMapping("/yek-valmistumispyynnot")
    fun getYekValmistumispyynnot(
        principal: Principal?
    ): ResponseEntity<List<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val erikoisalaFilter = LongFilter()
        erikoisalaFilter.equals = YEK_ERIKOISALA_ID
        return ResponseEntity.ok(
            valmistumispyyntoService.findAllForVastuuhenkiloByCriteria(
                user.id.required(),
                NimiErikoisalaAndAvoinCriteria(avoin = true, erikoisalaId = erikoisalaFilter),
                Pageable.unpaged()
            ).content
        )
    }

    @GetMapping("/seurantajaksot")
    fun getSeurantajaksot(
        principal: Principal?
    ): ResponseEntity<List<EtusivuSeurantajaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            seurantajaksoService.findAvoinByKouluttajaUserId(user.id.required())
        )
    }

    @GetMapping("/yliopisto")
    fun getYliopisto(principal: Principal?): ResponseEntity<String> {
        val userId = userService.getAuthenticatedUser(principal).id.required()
        val yliopistoNimi =
            kayttajaService.findByUserId(userId).get().yliopistotAndErikoisalat?.firstOrNull()?.yliopisto?.nimi
        return ResponseEntity.ok(yliopistoNimi)
    }
}
