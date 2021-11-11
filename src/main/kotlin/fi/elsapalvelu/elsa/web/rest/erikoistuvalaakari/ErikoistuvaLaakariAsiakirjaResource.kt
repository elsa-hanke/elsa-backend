package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "asiakirja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariAsiakirjaResource(
    private val userService: UserService,
    private val asiakirjaService: AsiakirjaService,
    private val fileValidationService: FileValidationService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/asiakirjat")
    fun createAsiakirjat(
        @Valid @RequestParam files: List<MultipartFile>,
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)

        if (!fileValidationService.validate(files, user.id!!)) {
            throw BadRequestAlertException(
                "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                ENTITY_NAME,
                "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
            )
        }

        val asiakirjat = files.map { it.mapAsiakirja() }
        val result = asiakirjaService.create(asiakirjat, user.id!!)
        return ResponseEntity
            .created(URI("/api/asiakirjat"))
            .body(result)
    }

    @GetMapping("/asiakirjat")
    fun getAllAsiakirjat(
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirjat = asiakirjaService.findAllByErikoistuvaLaakariUserId(user.id!!)

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/nimet")
    fun getReservedAsiakirjaNimet(
        principal: Principal?
    ): ResponseEntity<List<String>> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirjat = asiakirjaService.findAllByErikoistuvaLaakariUserId(user.id!!).map {
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
        val asiakirja = asiakirjaService.findOne(id, user.id!!)

        if (asiakirja != null) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asiakirja.nimi + "\"")
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
        asiakirjaService.delete(id, user.id!!)
        return ResponseEntity
            .noContent()
            .build()
    }
}

