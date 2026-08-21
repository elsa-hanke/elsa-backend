package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.web.rest.toFileDownloadResponse
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.security.Principal
import fi.elsapalvelu.elsa.audit.AuditLoggingWrapper
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.web.rest.VALMISTUMISPYYNTO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid


@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloValmistumispyyntoResource(
    private val userService: UserService,
    private val valmistumispyyntoService: ValmistumispyyntoService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @GetMapping("/valmistumispyynnot")
    fun getAllValmistumispyynnot(
        criteria: NimiErikoisalaAndAvoinCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<ValmistumispyyntoListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynnot =
            valmistumispyyntoService.findAllForVastuuhenkiloByCriteria(user.id.required(), criteria, pageable)

        return ResponseEntity.ok(valmistumispyynnot)
    }

    @GetMapping("/valmistumispyynnon-arviointi/{id}")
    fun getValmistumispyyntoOsaamisenArviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoOsaamisenArviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.findOneByIdAndVastuuhenkiloOsaamisenArvioijaUserId(id, user.id.required())

        return ResponseEntity.ok(valmistumispyynto)
    }

    @GetMapping("/valmistumispyynnon-hyvaksynta/{id}")
    fun getValmistumispyyntoHyvaksyja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valmistumispyynto =
            valmistumispyyntoService.findOneByIdAndVastuuhenkiloHyvaksyjaUserId(id, user.id.required())

        return ResponseEntity.ok(valmistumispyynto)
    }

    @GetMapping("/valmistumispyynto-arviointien-tila/{id}")
    fun getValmistumispyyntoArviointienTila(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ValmistumispyyntoArviointienTilaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val arviointienTila =
            valmistumispyyntoService.findArviointienTilaByIdAndOsaamisenArvioijaUserId(id, user.id.required())

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

        if (!valmistumispyyntoService.onkoAvoinOsaamisenTarkistaminen(user.id.required(), id)) {
            throw BadRequestAlertException(
                "Valmistumispyyntö ei ole muokattavissa.",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.valmistumispyynto-ei-ole-muokattavissa")
        }

        val valmistumispyynto =
            valmistumispyyntoService.updateOsaamisenArviointiByOsaamisenArvioijaUserId(
                id,
                user.id.required(),
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
        AuditLoggingWrapper.info("PUT request for /api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/$id")

        if (!valmistumispyyntoService.onkoAvoinHyvaksyja(user.id.required(), id)) {
            throw BadRequestAlertException(
                "Valmistumispyyntö ei ole muokattavissa.",
                VALMISTUMISPYYNTO_ENTITY_NAME,
                "dataillegal.valmistumispyynto-ei-ole-muokattavissa")
        }

        try {
            val valmistumispyynto =
                valmistumispyyntoService.updateValmistumispyyntoByHyvaksyjaUserId(
                    id,
                    user.id.required(),
                    hyvaksyntaFormDTO
            )
            AuditLoggingWrapper.info("PUT request completed for /api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/$id")
            return ResponseEntity.ok(valmistumispyynto)
        } catch (ex: InvalidPdfAttachmentException) {
            throw ex
        } catch (ex: Exception) {
            log.error("PUT request failed for /api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/$id", ex)
            throw ex
        }
    }

    @GetMapping("/valmistumispyynto/{valmistumispyyntoId}/asiakirja/{asiakirjaId}")
    fun getValmistumispyynnonAsiakirja(
        @PathVariable valmistumispyyntoId: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = valmistumispyyntoService.getValmistumispyynnonAsiakirja(user.id.required(), valmistumispyyntoId, asiakirjaId)

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/valmistumispyynto/{valmistumispyyntoId}/tyoskentelyjakso-liite/{asiakirjaId}")
    fun getValmistumispyyntoTyoskentelyjaksoLiite(
        @PathVariable valmistumispyyntoId: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = valmistumispyyntoService.getValmistumispyynnonTyoskentelyjaksoAsiakirja(user.id.required(), valmistumispyyntoId, asiakirjaId)

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            ?: ResponseEntity.notFound().build()
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
