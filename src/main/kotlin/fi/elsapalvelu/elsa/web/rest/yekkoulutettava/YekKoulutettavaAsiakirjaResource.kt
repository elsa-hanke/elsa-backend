package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.URLEncoder
import java.security.Principal

private const val ASIAKIRJA_ENTITY_NAME = "asiakirja"

@RestController
@RequestMapping("/api/yek-koulutettava")
class YekKoulutettavaAsiakirjaResource(
  private val userService: UserService,
  private val asiakirjaService: AsiakirjaService,
  private val fileValidationService: FileValidationService,
  private val opintooikeusService: OpintooikeusService,
  private val valmistumispyyntoService: ValmistumispyyntoService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/asiakirjat")
    fun createAsiakirjat(
      @Valid @RequestParam files: List<MultipartFile>,
      principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                user.id!!, YEK_ERIKOISALA_ID
            )

        if (!fileValidationService.validate(files, opintooikeusId)) {
            throw BadRequestAlertException(
              "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
              ASIAKIRJA_ENTITY_NAME,
              "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
            )
        }

        val asiakirjat = files.map { it.mapAsiakirja() }
        asiakirjaService.create(asiakirjat, opintooikeusId)?.let {
            return ResponseEntity.created(URI("/api/asiakirjat"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/asiakirjat")
    fun getAllAsiakirjat(
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                user.id!!, YEK_ERIKOISALA_ID
            )
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                user.id!!, YEK_ERIKOISALA_ID
            )
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
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                user.id!!, YEK_ERIKOISALA_ID
            )
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

        asiakirja?.asiakirjaData?.fileInputStream?.use {
            return ResponseEntity.ok()
                .header(
                  HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(asiakirja.nimi, "UTF-8") + "\""
                )
                .header(HttpHeaders.CONTENT_TYPE, asiakirja.tyyppi + "; charset=UTF-8")
                .body(it.readBytes())
        }

        return ResponseEntity.notFound().build()
    }

    @DeleteMapping("/asiakirjat/{id}")
    fun deleteAsiakirja(
      @PathVariable id: Long,
      principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId =
            opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                user.id!!, YEK_ERIKOISALA_ID
            )
        asiakirjaService.delete(id, opintooikeusId)
        return ResponseEntity.noContent()
            .build()
    }
    
}
