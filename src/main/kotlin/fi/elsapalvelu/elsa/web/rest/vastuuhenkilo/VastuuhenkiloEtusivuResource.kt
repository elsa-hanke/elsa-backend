package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.EtusivuService
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
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
    private val koejaksonVaiheetService: KoejaksonVaiheetService
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
}
