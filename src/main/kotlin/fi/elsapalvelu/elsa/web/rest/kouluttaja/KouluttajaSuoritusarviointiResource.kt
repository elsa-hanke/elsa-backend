package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

@RestController
@RequestMapping("/api/kouluttaja")
class KouluttajaSuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val arviointityokaluService: ArviointityokaluService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val login = user.login!!

        return ResponseEntity.ok(
            suoritusarviointiQueryService.findByKouluttajaUserLogin(login)
        )
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarviointiDTO = suoritusarviointiService
            .findOneByIdAndArvioinninAntajauserLogin(id, user.login!!)
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
        val user = userService.getAuthenticatedUser(principal)
        val result = suoritusarviointiService.save(suoritusarviointiDTO, user.login!!)
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

    @GetMapping("/suoritusarvioinnit/arviointityokalut")
    fun getArviointityokalut(
        principal: Principal?
    ): ResponseEntity<List<ArviointityokaluDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val arviointityokaluDTO = arviointityokaluService
            .findAllByKayttajaUserLogin(user.login!!)
        return ResponseEntity.ok(arviointityokaluDTO)
    }
}
