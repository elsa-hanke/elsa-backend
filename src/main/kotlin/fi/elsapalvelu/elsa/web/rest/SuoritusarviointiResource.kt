package fi.elsapalvelu.elsa.web.rest

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.FileValidationService
import fi.elsapalvelu.elsa.service.SuoritusarviointiQueryService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import tech.jhipster.web.util.ResponseUtil
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

open class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    private val fileValidationService: FileValidationService
) {

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarvioinnit =
            suoritusarviointiQueryService.findByKouluttajaOrVastuuhenkiloUserId(user.id!!)
        val avoimet = suoritusarvioinnit.filter { it.arviointiAika == null }
        val muut = suoritusarvioinnit.filter { it.arviointiAika != null }
        val sortedSuoritusarvioinnit =
            avoimet.sortedBy { it.tapahtumanAjankohta } + muut.sortedByDescending { it.tapahtumanAjankohta }

        return ResponseEntity.ok(sortedSuoritusarvioinnit)
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarviointiDTO =
            suoritusarviointiService.findOneByIdAndArvioinninAntajauserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @GetMapping("/suoritusarvioinnit/{id}/arviointi-liite")
    fun getArviointiLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = suoritusarviointiService
            .findAsiakirjaBySuoritusarviointiIdAndArvioinninAntajauserId(id, user.id!!)

        if (asiakirja != null) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asiakirja.nimi + "\"")
                .header(HttpHeaders.CONTENT_TYPE, asiakirja.tyyppi + "; charset=UTF-8")
                .body(asiakirja.asiakirjaData?.fileInputStream?.readBytes())
        }
        return ResponseEntity.notFound().build()
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestParam suoritusarviointiJson: String,
        @Valid @RequestParam arviointiFile: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        suoritusarviointiJson.let {
            objectMapper.readValue(it, SuoritusarviointiDTO::class.java)
        }?.let { suoritusarviointiDTO ->
            validateDTO(suoritusarviointiDTO)
            suoritusarviointiDTO.arviointiAsiakirja = getMappedFile(arviointiFile, user.id!!)
            val result = suoritusarviointiService.save(suoritusarviointiDTO, user.id!!)
            return ResponseEntity.ok(result)
        } ?: throw BadRequestAlertException(
            "Arvioinnin tallentaminen epäonnistui.",
            ENTITY_NAME,
            "dataillegal.arvioinnin-tallentaminen-epaonnistui"
        )
    }

    private fun getMappedFile(arviointiFile: MultipartFile?, userId: String): AsiakirjaDTO? {
        return arviointiFile?.let {
            if (!fileValidationService.validate(listOf(it), userId, listOf(MediaType.APPLICATION_PDF_VALUE))) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.mapAsiakirja()
        }
    }

    private fun validateDTO(suoritusarviointiDTO: SuoritusarviointiDTO) {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        if (suoritusarviointiDTO.arviointiasteikonTaso == null
            || suoritusarviointiDTO.sanallinenArviointi == null
        ) {
            throw BadRequestAlertException(
                "Kouluttajan arvioinnin täytyy sisältää arviointiasteikon taso ja sanallinen arviointi",
                ENTITY_NAME,
                "dataillegal.kouluttajan-arvioinnin-taytyy-sisaltaa-arviointiasteikon-taso-ja-sanallinen-arviointi"
            )
        }
    }
}
