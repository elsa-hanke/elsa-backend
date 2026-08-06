package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.KayttajaService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoListItemDTO
import fi.elsapalvelu.elsa.web.rest.VALMISTUMISPYYNTO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.toFileDownloadResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal


@RestController
@RequestMapping("/api/virkailija")
class VirkailijaValmistumispyyntoResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val valmistumispyyntoService: ValmistumispyyntoService,
    private val asiakirjaService: AsiakirjaService
) {
    @GetMapping("/valmistumispyynnot")
    fun getAllValmistumispyynnot(
        criteria: NimiErikoisalaAndAvoinCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynnot =
            valmistumispyyntoService.findAllForVirkailijaByCriteria(
                user.id!!,
                criteria,
                criteria.erikoisalaId?.let { listOf(it.equals) } ?: listOf(),
                if (criteria.erikoisalaId?.equals == YEK_ERIKOISALA_ID) listOf() else listOf(YEK_ERIKOISALA_ID),
                pageable
            )

        return ResponseEntity.ok(valmistumispyynnot)
    }

    @GetMapping("/valmistumispyynnon-tarkistus/{id}")
    fun getValmistumispyynnonTarkistus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val tarkistus =
            valmistumispyyntoService.findOneByIdAndVirkailijaUserId(id, user.id!!)

        return ResponseEntity.ok(tarkistus)
    }

    @PutMapping("/valmistumispyynnon-tarkistus/{id}")
    fun updateValmistumispyynnonTarkistus(
        @PathVariable(value = "id", required = true) id: Long,
        @Valid valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO,
        @RequestParam(required = false) laillistamistodistus: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (!valmistumispyyntoService.onkoAvoinVirkailija(user.id!!, id)) {
            throw BadRequestAlertException(
                "Valmistumispyyntö ei ole muokattavissa.",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.valmistumispyynto-ei-ole-muokattavissa")
        }

        val tarkistus =
            valmistumispyyntoService.updateTarkistusByVirkailijaUserId(
                id,
                user.id!!,
                valmistumispyynnonTarkistusDTO,
                laillistamistodistus
            )

        return ResponseEntity.ok(tarkistus)
    }

    @GetMapping("/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}")
    fun getValmistumispyynnonAsiakirja(
        @PathVariable valmistumispyyntoId: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        val asiakirja = valmistumispyyntoService.getValmistumispyynnonAsiakirjaVirkailija(
            valmistumispyyntoId,
            kayttaja.orElse(null)?.yliopistot?.firstOrNull()?.id,
            asiakirjaId
        )

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi ?: "", asiakirja.tyyppi ?: "")
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/valmistumispyynto/tyoskentelyjakso-liite/{id}")
    fun getValmistumispyyntoTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        val asiakirja = asiakirjaService
            .findByIdAndYliopistoId(
                id,
                kayttaja.orElse(null)?.yliopistot?.map { it.id!! })

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi ?: "", asiakirja.tyyppi ?: "")
            ?: ResponseEntity.notFound().build()
    }
}
