package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.web.rest.toFileDownloadResponse
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
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
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import jakarta.validation.Valid

private const val ENTITY_NAME = "asiakirja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariAsiakirjaResource(
    private val userService: UserService,
    private val asiakirjaService: AsiakirjaService,
    private val fileValidationService: FileValidationService,
    private val opintooikeusService: OpintooikeusService,
    private val valmistumispyyntoService: ValmistumispyyntoService
) {

    @PostMapping("/asiakirjat")
    fun createAsiakirjat(
        @Valid @RequestParam files: List<MultipartFile>,
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!fileValidationService.validate(files, opintooikeusId)) {
            throw BadRequestAlertException(
                "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                ENTITY_NAME,
                "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
            )
        }

        val asiakirjat = files.map { it.mapAsiakirja() }
        return asiakirjaService.create(asiakirjat, opintooikeusId)?.let {
            ResponseEntity
                .created(URI("/api/asiakirjat"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/asiakirjat")
    fun getAllAsiakirjat(
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        var asiakirjat = asiakirjaService.findAllByOpintooikeusId(opintooikeusId)

        val authorities =
            (principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
        if (authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) || authorities.contains(
                ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
            )
        ) {
            valmistumispyyntoService.findOneByOpintooikeusId(opintooikeusId)?.let {
                asiakirjat = asiakirjat.filter { a -> it.erikoistujanTiedotAsiakirjaId != a.id }
            }
        }

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/nimet")
    fun getReservedAsiakirjaNimet(
        principal: Principal?
    ): ResponseEntity<List<String>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val asiakirjat = asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map {
            it.nimi!!
        }

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/{id}")
    fun getAsiakirja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val asiakirja = asiakirjaService.findOne(id, opintooikeusId)

        val authorities =
            (principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)
        if (authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) || authorities.contains(
                ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
            )
        ) {
            valmistumispyyntoService.findOneByOpintooikeusId(opintooikeusId)?.let {
                if (it.erikoistujanTiedotAsiakirjaId == id) {
                    return ResponseEntity.notFound().build()
                }
            }
        }

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/asiakirjat/{id}")
    fun deleteAsiakirja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        asiakirjaService.delete(id, opintooikeusId)
        return ResponseEntity
            .noContent()
            .build()
    }
}
