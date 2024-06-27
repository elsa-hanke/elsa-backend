package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.security.Principal
import jakarta.validation.Valid

private const val VALMISTUMISPYYNTO_ENTITY_NAME = "valmistumispyyntö"

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloValmistumispyyntoResource(
    private val userService: UserService,
    private val valmistumispyyntoService: ValmistumispyyntoService
) {
    @GetMapping("/valmistumispyynnot")
    fun getAllValmistumispyynnot(
        criteria: NimiErikoisalaAndAvoinCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynnot =
            valmistumispyyntoService.findAllForVastuuhenkiloByCriteria(user.id!!, criteria, pageable)

        return ResponseEntity.ok(valmistumispyynnot)
    }

    @GetMapping("/valmistumispyynnon-arviointi/{id}")
    fun getValmistumispyyntoOsaamisenArviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoOsaamisenArviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.findOneByIdAndVastuuhenkiloOsaamisenArvioijaUserId(id, user.id!!)

        return ResponseEntity.ok(valmistumispyynto)
    }

    @GetMapping("/valmistumispyynnon-hyvaksynta/{id}")
    fun getValmistumispyyntoHyvaksyja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.findOneByIdAndVastuuhenkiloHyvaksyjaUserId(id, user.id!!)

        return ResponseEntity.ok(valmistumispyynto)
    }

    @GetMapping("/valmistumispyynto-arviointien-tila/{id}")
    fun getValmistumispyyntoArviointienTila(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoArviointienTilaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val arviointienTila =
            valmistumispyyntoService.findArviointienTilaByIdAndOsaamisenArvioijaUserId(id, user.id!!)

        return ResponseEntity.ok(arviointienTila)
    }

    @PutMapping("/valmistumispyynnon-arviointi/{id}")
    fun updateValmistumispyynto(
        @PathVariable id: Long,
        @Valid @RequestBody osaamisenArviointiDTO: ValmistumispyyntoOsaamisenArviointiFormDTO,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoDTO> {
        validateOsaamisenArviointiDto(osaamisenArviointiDTO)

        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.updateOsaamisenArviointiByOsaamisenArvioijaUserId(
                id,
                user.id!!,
                osaamisenArviointiDTO
            )

        return ResponseEntity.ok(valmistumispyynto)
    }

    @PutMapping("/valmistumispyynnon-hyvaksynta/{id}")
    fun updateValmistumispyyntoHyvaksyja(
        @PathVariable id: Long,
        @Valid @RequestBody hyvaksyntaFormDTO: ValmistumispyyntoHyvaksyntaFormDTO,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {

        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.updateValmistumispyyntoByHyvaksyjaUserId(
                id,
                user.id!!,
                hyvaksyntaFormDTO
            )

        return ResponseEntity.ok(valmistumispyynto)
    }

    @GetMapping("/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}")
    fun getValmistumispyynnonAsiakirja(
        @PathVariable valmistumispyyntoId: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = valmistumispyyntoService.getValmistumispyynnonAsiakirja(user.id!!, valmistumispyyntoId, asiakirjaId)

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

    @GetMapping("/valmistumispyynto/{valmistumispyyntoId}/tyoskentelyjakso-liite/{asiakirjaId}")
    fun getValmistumispyyntoTyoskentelyjaksoLiite(
        @PathVariable valmistumispyyntoId: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = valmistumispyyntoService.getValmistumispyynnonTyoskentelyjaksoAsiakirja(user.id!!, valmistumispyyntoId, asiakirjaId)

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

    private fun validateOsaamisenArviointiDto(osaamisenArviointiDTO: ValmistumispyyntoOsaamisenArviointiFormDTO) {
        if ((osaamisenArviointiDTO.osaaminenRiittavaValmistumiseen == null ||
                osaamisenArviointiDTO.osaaminenRiittavaValmistumiseen == false) &&
            osaamisenArviointiDTO.korjausehdotus == null
        ) {
            throw BadRequestAlertException(
                "Lisätiedot erikoistujalle vaaditaan",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.lisatiedot-erikoistujalle-vaaditaan"
            )
        }
    }
}
