package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.security.Principal
import jakarta.validation.Valid

private const val VALMISTUMISPYYNTO_ENTITY_NAME = "valmistumispyynto"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariValmistumispyyntoResource(
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val valmistumispyyntoService: ValmistumispyyntoService,
    private val fileValidationService: FileValidationService
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
        @Valid uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        @RequestParam(required = false) laillistamistodistus: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhatSuorituksetDTO =
            valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        validateLaillistamispaivaAndTodistus(user, uusiValmistumispyyntoDTO, laillistamistodistus)
        validateVanhentuneetSuoritukset(uusiValmistumispyyntoDTO, vanhatSuorituksetDTO)
        validateValmistumispyyntoNotExists(opintooikeusId)
        validateLaillistamistodistusIfExists(laillistamistodistus)

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            uusiValmistumispyyntoDTO.laillistamispaiva,
            laillistamistodistus?.bytes,
            laillistamistodistus?.originalFilename,
            laillistamistodistus?.contentType
        )

        valmistumispyyntoService.create(opintooikeusId, uusiValmistumispyyntoDTO).let { result ->
            return ResponseEntity
                .created(URI("/api/valmistumispyynto"))
                .body(result)
        }
    }

    @PutMapping("/valmistumispyynto")
    fun updateValmistumispyynto(
        @Valid uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        @RequestParam(required = false) laillistamistodistus: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val erikoisalaTyyppi = valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(opintooikeusId)
        val vanhatSuorituksetDTO =
            valmistumispyyntoService.findSuoritustenTila(opintooikeusId, erikoisalaTyyppi)

        validateLaillistamispaivaAndTodistus(user, uusiValmistumispyyntoDTO, laillistamistodistus)
        validateVanhentuneetSuoritukset(uusiValmistumispyyntoDTO, vanhatSuorituksetDTO)
        validateLaillistamistodistusIfExists(laillistamistodistus)

        if (valmistumispyyntoService.onkoLahetetty(opintooikeusId)) {
            throw BadRequestAlertException(
                "Lähetettyä valmistumispyyntöä ei saa muokata.",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.lahetettya-valmistumispyyntoa-ei-saa-muokata")
        }

        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            uusiValmistumispyyntoDTO.laillistamispaiva,
            laillistamistodistus?.bytes,
            laillistamistodistus?.originalFilename,
            laillistamistodistus?.contentType
        )

        valmistumispyyntoService.update(opintooikeusId, uusiValmistumispyyntoDTO).let { result ->
            return ResponseEntity.ok(result)
        }
    }

    private fun validateValmistumispyyntoNotExists(opintooikeusId: Long) {
        if (valmistumispyyntoService.existsByOpintooikeusId(opintooikeusId)) {
            throw BadRequestAlertException(
                "Valmistumispyyntö on jo lähetetty",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.valmistumispyynto-on-jo-lahetetty"
            )
        }
    }

    private fun validateLaillistamispaivaAndTodistus(
        user: UserDTO,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        laillistamistodistus: MultipartFile?
    ) {
        if ((uusiValmistumispyyntoDTO.laillistamispaiva == null || laillistamistodistus == null) &&
            !erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(
                user.id!!
            )
        ) {
            throw BadRequestAlertException(
                "Laillistamispaiva ja todistus vaaditaan",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.laillistamispaiva-ja-todistus-vaaditaan"
            )
        }
    }

    private fun validateVanhentuneetSuoritukset(
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO,
        vanhatSuorituksetDTO: VanhentuneetSuorituksetDTO
    ) {
        if (uusiValmistumispyyntoDTO.selvitysVanhentuneistaSuorituksista == null &&
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

    private fun validateLaillistamistodistusIfExists(laillistamistodistus: MultipartFile?) {
        laillistamistodistus?.let {
            if (!fileValidationService.validate(listOf(it))) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen.",
                    VALMISTUMISPYYNTO_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen"
                )
            }
        }
    }
}
