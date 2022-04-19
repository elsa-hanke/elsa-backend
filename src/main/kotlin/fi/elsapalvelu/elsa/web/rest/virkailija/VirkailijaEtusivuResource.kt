package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaOptionsVirkailijaDTO
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
    private val etusivuService: EtusivuService
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

    @GetMapping("/yliopisto")
    fun getYliopisto(principal: Principal?): ResponseEntity<String> {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val yliopistoNimi = kayttajaService.findByUserId(userId).get().yliopistot?.firstOrNull()?.nimi
        return ResponseEntity.ok(yliopistoNimi)
    }
}
