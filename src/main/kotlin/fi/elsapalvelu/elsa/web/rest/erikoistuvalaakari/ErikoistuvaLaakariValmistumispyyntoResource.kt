package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoSuoritustenTilaDTO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoApplicationService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.security.Principal

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariValmistumispyyntoResource(
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val valmistumispyyntoApplicationService: ValmistumispyyntoApplicationService
) {

    @GetMapping("/valmistumispyynto")
    fun getValmistumispyynto(principal: Principal?): ResponseEntity<ValmistumispyyntoDTO> {
        val context = resolveContext(principal)

        return ResponseEntity.ok(valmistumispyyntoApplicationService.findOne(context.opintooikeusId))
    }

    @GetMapping("/valmistumispyynto-suoritusten-tila")
    fun getValmistumispyyntoSuoritustenTila(
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoSuoritustenTilaDTO> {
        val context = resolveContext(principal)

        return ResponseEntity.ok(
            valmistumispyyntoApplicationService.findSuoritustenTila(context.opintooikeusId)
        )
    }

    @PostMapping("/valmistumispyynto")
    fun createValmistumispyynto(
        @Valid uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        @RequestParam(required = false) laillistamistodistus: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val context = resolveContext(principal)
        val result = valmistumispyyntoApplicationService.create(
            context.userId,
            context.opintooikeusId,
            uusiValmistumispyyntoDTO,
            laillistamistodistus
        )

        return ResponseEntity.created(URI("/api/valmistumispyynto")).body(result)
    }

    @PutMapping("/valmistumispyynto")
    fun updateValmistumispyynto(
        @Valid uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        @RequestParam(required = false) laillistamistodistus: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val context = resolveContext(principal)
        val result = valmistumispyyntoApplicationService.updateWhenNotSent(
            context.userId,
            context.opintooikeusId,
            uusiValmistumispyyntoDTO,
            laillistamistodistus
        )

        return ResponseEntity.ok(result)
    }

    private fun resolveContext(principal: Principal?): ValmistumispyyntoContext {
        val userId = userService.getAuthenticatedUser(principal).id.required()
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId)

        return ValmistumispyyntoContext(userId, opintooikeusId)
    }

    private data class ValmistumispyyntoContext(
        val userId: String,
        val opintooikeusId: Long
    )
}
