package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.URLEncoder
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "asiakirja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariAsiakirjaResource(
    private val userService: UserService,
    private val asiakirjaService: AsiakirjaService,
    private val fileValidationService: FileValidationService,
    private val opintooikeusService: OpintooikeusService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/asiakirjat")
    fun createAsiakirjat(
        @Valid @RequestParam files: List<MultipartFile>,
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        if (!fileValidationService.validate(files, opintooikeusId)) {
            throw BadRequestAlertException(
                "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                ENTITY_NAME,
                "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
            )
        }

        val asiakirjat = files.map { it.mapAsiakirja() }
        asiakirjaService.create(asiakirjat, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/asiakirjat"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/asiakirjat")
    fun getAllAsiakirjat(
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val asiakirjat = asiakirjaService.findAllByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/nimet")
    fun getReservedAsiakirjaNimet(
        principal: Principal?
    ): ResponseEntity<List<String>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
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
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val asiakirja = asiakirjaService.findOne(id, opintooikeusId)

        if (asiakirja != null) {
            return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(asiakirja.nimi, "UTF-8") + "\""
                )
                .header(HttpHeaders.CONTENT_TYPE, asiakirja.tyyppi + "; charset=UTF-8")
                .body(asiakirja.asiakirjaData?.fileInputStream?.readBytes())
        }
        return ResponseEntity.notFound().build()
    }

    @DeleteMapping("/asiakirjat/{id}")
    fun deleteAsiakirja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        asiakirjaService.delete(id, opintooikeusId)
        return ResponseEntity
            .noContent()
            .build()
    }
}

