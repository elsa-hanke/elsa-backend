package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.SuoritusarviointiQueryService
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

open class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val kayttajaService: KayttajaService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val suoritusarvioinnit =
            suoritusarviointiQueryService.findByKouluttajaOrVastuuhenkiloId(kayttaja.id!!)
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
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val suoritusarviointiDTO =
            suoritusarviointiService.findOneByIdAndArvioinninAntajaId(id, kayttaja.id!!)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        if (suoritusarviointiDTO.vaativuustaso == null
            || suoritusarviointiDTO.luottamuksenTaso == null
            || suoritusarviointiDTO.sanallinenArviointi == null
        ) {
            throw BadRequestAlertException(
                "Kouluttajan arvioinnin täytyy sisältää vaativuustaso, luottamuksen taso ja sanallien arviointi",
                ENTITY_NAME,
                "dataillegal"
            )
        }
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val result = suoritusarviointiService.save(suoritusarviointiDTO, kayttaja.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    suoritusarviointiDTO.id.toString()
                )
            )
            .body(result)
    }
}
