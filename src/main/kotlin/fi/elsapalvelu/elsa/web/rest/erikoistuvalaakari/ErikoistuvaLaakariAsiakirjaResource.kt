package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.FileValidatorService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import io.github.jhipster.web.util.HeaderUtil
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
    private val kayttajaService: KayttajaService,
    private val asiakirjaService: AsiakirjaService,
    private val fileValidator: FileValidatorService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/asiakirjat")
    fun createAsiakirjat(
        @Valid @RequestParam files: List<MultipartFile>,
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        fileValidator.validate(files, kayttaja.id!!)

        val asiakirjat =
            files.map {
                AsiakirjaDTO(
                    nimi = it.originalFilename,
                    tyyppi = it.contentType,
                    asiakirjaData = AsiakirjaDataDTO(
                        fileInputStream = it.inputStream,
                        fileSize = it.size
                    )
                )
            }

        val result = asiakirjaService.create(asiakirjat, kayttaja.id!!)
        return ResponseEntity.created(URI("/api/asiakirjat"))
            .headers(
                HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    result?.size.toString()
                )
            )
            .body(result)
    }

    @GetMapping("/asiakirjat")
    fun getAllAsiakirjat(
        principal: Principal?
    ): ResponseEntity<List<AsiakirjaDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val asiakirjat = asiakirjaService.findAllByErikoistuvaLaakariId(kayttaja.id!!)

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/nimet")
    fun getReservedAsiakirjaNimet(
        principal: Principal?
    ): ResponseEntity<List<String>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val asiakirjat = asiakirjaService.findAllByErikoistuvaLaakariId(kayttaja.id!!).map {
            it.nimi!!
        }

        return ResponseEntity.ok(asiakirjat)
    }

    @GetMapping("/asiakirjat/{id}")
    fun getAsiakirja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val asiakirja = asiakirjaService.findOne(id, kayttaja.id!!)

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
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        asiakirjaService.delete(id, kayttaja.id!!)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    "asiakirja",
                    id.toString()
                )
            )
            .build()
    }
}

