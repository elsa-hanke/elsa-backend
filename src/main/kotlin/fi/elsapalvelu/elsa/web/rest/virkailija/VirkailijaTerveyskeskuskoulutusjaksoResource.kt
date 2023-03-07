package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.security.Principal
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException

private const val TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME = "terveyskeskuskoulutusjakson_hyvaksynta"

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaTerveyskeskuskoulutusjaksoResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val asiakirjaService: AsiakirjaService
) {

    @GetMapping("/terveyskeskuskoulutusjaksot")
    fun getTerveyskeskuskoulutusjaksot(
        principal: Principal?,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): ResponseEntity<Page<TerveyskeskuskoulutusjaksoSimpleDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByVirkailijaUserId(
                user.id!!,
                criteria,
                pageable
            )
        )
    }

    @GetMapping("/terveyskeskuskoulutusjakso/{id}")
    fun getTerveyskeskuskoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!).get()
        val yliopistoIds = kayttaja.yliopistot?.map { it.id!! }.orEmpty().toList()
        try {
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByIdAndYliopistoIdVirkailija(
                id,
                yliopistoIds
            )
                .let {
                    if (it == null) return ResponseEntity.notFound().build()
                    return ResponseEntity.ok(it)
                }
        } catch (e: EntityNotFoundException) {
            throw BadRequestAlertException(
                "Vastuuhenkilöä ei löytynyt",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.vastuuhenkiloa-ei-loytynyt"
            )
        } catch (e: ValidationException) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson vähimmäispituus ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-vahimmaispituus-ei-tayty"
            )
        }
    }

    @GetMapping("/terveyskeskuskoulutusjakso/tyoskentelyjakso-liite/{id}")
    fun getTerveyskeskuskoulutusjaksoTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        val asiakirja = asiakirjaService
            .findByIdAndTyoskentelyjaksoTyyppi(
                id,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                kayttaja.orElse(null)?.yliopistot?.map { it.id!! })

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

    @PutMapping("/terveyskeskuskoulutusjakson-hyvaksynta/{id}")
    fun updateTerveyskeskuskoulutusjaksonHyvaksynta(
        @PathVariable id: Long,
        @RequestBody dto: TerveyskeskuskoulutusjaksoUpdateDTO?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)

        terveyskeskuskoulutusjaksonHyvaksyntaService.update(
            user.id!!,
            true,
            id,
            dto?.korjausehdotus,
            dto?.lisatiedotVirkailijalta
        )
            .let {
                return ResponseEntity.ok(it)
            }
    }
}
