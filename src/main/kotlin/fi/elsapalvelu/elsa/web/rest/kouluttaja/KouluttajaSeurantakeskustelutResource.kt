package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.service.SeurantajaksoService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO
import fi.elsapalvelu.elsa.service.dto.SeurantajaksonTiedotDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.util.*
import jakarta.validation.Valid

private const val ENTITY_NAME = "seurantajakso"

@RestController
@RequestMapping("/api/kouluttaja/seurantakeskustelut")
class KouluttajaSeurantakeskustelutResource(
    private val userService: UserService,
    private val seurantajaksoService: SeurantajaksoService
) {

    @GetMapping("/seurantajaksot")
    fun getSeurantajaksot(principal: Principal?): ResponseEntity<List<SeurantajaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val seurantajaksot =
            seurantajaksoService.findByKouluttajaUserId(user.id!!).groupBy { it.opintooikeusId }
        val result = mutableListOf<SeurantajaksoDTO>()
        seurantajaksot.forEach { (_, erikoistuvanJaksot) ->
            val sortedJaksot = sortSeurantajaksot(erikoistuvanJaksot)
            val uusin = sortedJaksot.first()
            uusin.aiemmatJaksot = sortedJaksot.filter { it.id != uusin.id }
            result.add(uusin)
        }

        return ResponseEntity.ok(sortSeurantajaksot(result))
    }

    @GetMapping("/seurantajakso/{id}")
    fun getSeurantajakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SeurantajaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return seurantajaksoService.findByIdAndKouluttajaUserId(id, user.id!!)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/seurantajaksontiedot")
    fun getSeurantajaksonTiedot(
        principal: Principal?,
        @RequestParam id: Long
    ): ResponseEntity<SeurantajaksonTiedotDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(seurantajaksoService.findSeurantajaksonTiedot(id, user.id!!))
    }

    @PutMapping("/seurantajakso/{id}")
    fun updateSeurantajakso(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid @RequestBody seurantajaksoDTO: SeurantajaksoDTO,
        principal: Principal?
    ): ResponseEntity<SeurantajaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (seurantajaksoDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_NAME, "idnull"
            )
        }

        if (!Objects.equals(id, seurantajaksoDTO.id)) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_NAME,
                "idinvalid"
            )
        }

        val seurantajakso = seurantajaksoService.findByIdAndKouluttajaUserId(id, user.id!!)
            ?: throw BadRequestAlertException(
                "Seurantajaksoa ei löydy",
                ENTITY_NAME,
                "idnotfound"
            )

        if (seurantajakso.hyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä seurantajaksoa ei saa päivittää",
                ENTITY_NAME,
                "dataillegal.hyvaksyttya-seurantajaksoa-ei-saa-paivittaa"
            )
        }

        val result = seurantajaksoService.update(seurantajaksoDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    private fun sortSeurantajaksot(jaksot: List<SeurantajaksoDTO>): List<SeurantajaksoDTO> {
        val avoimetVaiheet =
            jaksot.filter { isAvoin(it) }.sortedBy { it.tallennettu }.toList()
        val muutVaiheet =
            jaksot.filter { !isAvoin(it) }.sortedByDescending { it.tallennettu }.toList()
        return avoimetVaiheet + muutVaiheet
    }

    private fun isAvoin(seurantajaksoDTO: SeurantajaksoDTO): Boolean {
        return seurantajaksoDTO.tila in listOf(
            SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA,
            SeurantajaksoTila.ODOTTAA_ARVIOINTIA,
            SeurantajaksoTila.ODOTTAA_HYVAKSYNTAA
        )
    }
}
