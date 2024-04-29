package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.jhipster.service.filter.LongFilter
import java.security.Principal

@RestController
@RequestMapping("/api/vastuuhenkilo/etusivu")
class VastuuhenkiloEtusivuResource(
    private val userService: UserService,
    private val etusivuService: EtusivuService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val valmistumispyyntoService: ValmistumispyyntoService,
    private val seurantajaksoService: SeurantajaksoService
) {

    @GetMapping("/erikoistujien-seuranta-rajaimet")
    fun getErikoistujienSeurantaRajaimet(
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<ErikoistujienSeurantaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaVastuuhenkiloRajaimet(user.id!!))
    }

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<ErikoistujanEteneminenDTO>?> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaForVastuuhenkilo(user.id!!, criteria, pageable))
    }

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?
    ): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            koejaksonVaiheetService.findAllByVastuuhenkiloKayttajaUserId(
                user.id!!,
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
                user.id!!,
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
                user.id!!,
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
            seurantajaksoService.findAvoinByKouluttajaUserId(user.id!!)
        )
    }
}
