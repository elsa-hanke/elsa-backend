package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.EtusivuSeurantajaksoDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoListItemDTO
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        principal: Principal?
    ): ResponseEntity<ErikoistujienSeurantaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaForVastuuhenkilo(user.id!!))
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
