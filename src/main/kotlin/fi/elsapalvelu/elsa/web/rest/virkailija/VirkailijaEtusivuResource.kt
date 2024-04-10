package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaOptionsVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/virkailija/etusivu")
class VirkailijaEtusivuResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val erikoisalaService: ErikoisalaService,
    private val asetusService: AsetusService,
    private val etusivuService: EtusivuService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val valmistumispyyntoService: ValmistumispyyntoService
) {
    @GetMapping("/erikoistujien-seuranta-rajaimet")
    fun getErikoistujienSeurantaRajaimet(): ResponseEntity<ErikoistujienSeurantaOptionsVirkailijaDTO> {
        val form = ErikoistujienSeurantaOptionsVirkailijaDTO()
        form.erikoisalat = erikoisalaService.findAllByLiittynytElsaan().toSet()
        form.asetukset = asetusService.findAll().toSet()
        return ResponseEntity.ok(form)
    }

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<ErikoistujanEteneminenVirkailijaDTO>> {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val erikoistujat =
            etusivuService.getErikoistujienSeurantaForVirkailija(userId, criteria, pageable)
        return ResponseEntity.ok(erikoistujat)
    }

    @GetMapping("/koulutettavien-seuranta-rajaimet")
    fun getKoulutettavienSeurantaRajaimet(): ResponseEntity<ErikoistujienSeurantaOptionsVirkailijaDTO> {
        val form = ErikoistujienSeurantaOptionsVirkailijaDTO()
        form.erikoisalat = erikoisalaService.findAllByIdIs(YEK_ERIKOISALA_ID).toSet()
        form.asetukset = asetusService.findAll().toSet() // todo vaatii rajaimia kun asetus asiat selvillä
        return ResponseEntity.ok(form)
    }

    @GetMapping("/koulutettavien-seuranta")
    fun getKoulutettavienSeurantaList(
        criteria: ErikoistujanEteneminenCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<ErikoistujanEteneminenVirkailijaDTO>> {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val koulutettavat =
            etusivuService.getKoulutettavienSeurantaForVirkailija(userId, criteria, pageable)
        return ResponseEntity.ok(koulutettavat)
    }

    @GetMapping("/yliopisto")
    fun getYliopisto(principal: Principal?): ResponseEntity<String> {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val yliopistoNimi =
            kayttajaService.findByUserId(userId).get().yliopistot?.firstOrNull()?.nimi
        return ResponseEntity.ok(yliopistoNimi)
    }

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?
    ): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            koejaksonVaiheetService.findAllAvoinByVirkailijaKayttajaUserId(
                user.id!!
            )
        )
    }

    @GetMapping("/valmistumispyynnot")
    fun getValmistumispyynnot(
        principal: Principal?
    ): ResponseEntity<List<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            valmistumispyyntoService.findAllForVirkailijaByCriteria(
                user.id!!,
                NimiErikoisalaAndAvoinCriteria(avoin = true),
                listOf(),
                Pageable.unpaged()
            ).content
        )
    }

    @GetMapping("/koulutettavien-valmistumispyynnot")
    fun getKoulutettavienValmistumispyynnot(
        principal: Principal?
    ): ResponseEntity<List<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            valmistumispyyntoService.findAllForVirkailijaByCriteria(
                user.id!!,
                NimiErikoisalaAndAvoinCriteria(avoin = true),
                listOf(YEK_ERIKOISALA_ID),
                Pageable.unpaged()
            ).content
        )
    }

}
