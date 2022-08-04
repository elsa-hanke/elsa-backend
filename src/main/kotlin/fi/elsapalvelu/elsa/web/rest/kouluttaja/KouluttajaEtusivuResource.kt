package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.SeurantajaksoService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.EtusivuSeurantajaksoDTO
import fi.elsapalvelu.elsa.service.dto.KatseluoikeusDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/kouluttaja/etusivu")
class KouluttajaEtusivuResource(
    private val userService: UserService,
    private val etusivuService: EtusivuService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val seurantajaksoService: SeurantajaksoService
) {

    @GetMapping("/erikoistujien-seuranta")
    fun getErikoistujienSeurantaList(
        principal: Principal?
    ): ResponseEntity<ErikoistujienSeurantaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getErikoistujienSeurantaForKouluttaja(user.id!!))
    }

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?
    ): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            koejaksonVaiheetService.findAllByKouluttajaKayttajaUserId(
                user.id!!,
                true
            )
        )
    }

    @GetMapping("/vanhenevat-katseluoikeudet")
    fun getVanhenevatKatseluoikeudet(
        principal: Principal?
    ): ResponseEntity<List<KatseluoikeusDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(etusivuService.getVanhenevatKatseluoikeudetForKouluttaja(user.id!!))
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
