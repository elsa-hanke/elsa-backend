package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoErikoistujaSaveDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoSuoritustenTilaDTO
import fi.elsapalvelu.elsa.service.dto.VanhentuneetSuorituksetDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val VALMISTUMISPYYNTO_ENTITY_NAME = "valmistumispyyntö"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariValmistumispyyntoResource(
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val valmistumispyyntoService: ValmistumispyyntoService
) {

    @GetMapping("/valmistumispyynto")
    fun getValmistumispyynto(principal: Principal?): ResponseEntity<ValmistumispyyntoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        return ResponseEntity.ok(valmistumispyyntoService.findOneByOpintooikeusId(opintooikeusId))
    }

    @GetMapping("/valmistumispyynto-suoritusten-tila")
    fun getValmistumispyyntoSuoritustenTila(principal: Principal?): ResponseEntity<ValmistumispyyntoSuoritustenTilaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhatSuorituksetDTO =
            valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)
        val form = ValmistumispyyntoSuoritustenTilaDTO(
            erikoisalaTyyppi = erikoisalaTyyppi,
            vanhojaTyoskentelyjaksojaOrSuorituksiaExists = vanhatSuorituksetDTO.vanhojaTyoskentelyjaksojaOrSuorituksiaExists,
            kuulusteluVanhentunut = vanhatSuorituksetDTO.kuulusteluVanhentunut
        )

        return ResponseEntity.ok(form)
    }

    @PostMapping("/valmistumispyynto")
    fun createValmistumispyynto(
        @Valid @RequestBody valmistumispyyntoDTO: ValmistumispyyntoErikoistujaSaveDTO,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhatSuorituksetDTO =
            valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        validateVanhentuneetSuoritukset(valmistumispyyntoDTO, vanhatSuorituksetDTO)

        try {
            valmistumispyyntoService.create(opintooikeusId, valmistumispyyntoDTO)?.let { result ->
                return ResponseEntity
                    .created(URI("/api/valmistumispyynto"))
                    .body(result)
            } ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (ex: Exception) {
            throw BadRequestAlertException(
                ex.message ?: "",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.kaikkia-valmistumispyynnon-tarvitsemia-tietoja-ei-loytynyt"
            )
        }
    }

    @PutMapping("/valmistumispyynto")
    fun updateValmistumispyynto(
        @Valid @RequestBody valmistumispyyntoDTO: ValmistumispyyntoErikoistujaSaveDTO,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhatSuorituksetDTO =
            valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        validateVanhentuneetSuoritukset(valmistumispyyntoDTO, vanhatSuorituksetDTO)

        try {
            valmistumispyyntoService.update(opintooikeusId, valmistumispyyntoDTO)?.let { result ->
                return ResponseEntity.ok(result)
            } ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (ex: Exception) {
            throw BadRequestAlertException(
                ex.message ?: "",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.valmistumispyyntoa-ei-voitu-paivittaa"
            )
        }
    }

    private fun validateVanhentuneetSuoritukset(
        valmistumispyyntoDTO: ValmistumispyyntoErikoistujaSaveDTO,
        vanhatSuorituksetDTO: VanhentuneetSuorituksetDTO
    ) {
        if (valmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista == null &&
            (vanhatSuorituksetDTO.vanhojaTyoskentelyjaksojaOrSuorituksiaExists == true
                    || vanhatSuorituksetDTO.kuulusteluVanhentunut == true)
        ) {
            throw BadRequestAlertException(
                "Selvitys vanhentuneista suorituksista vaaditaan",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.selvitys-vanhentuneista-suorituksista-vaaditaan"
            )
        }
    }
}
