package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaValmistumispyyntoResource(
    private val userService: UserService,
    private val valmistumispyyntoService: ValmistumispyyntoService
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
        @Valid @RequestBody valmistumispyynnonTarkistusDTO: ValmistumispyynnonTarkistusUpdateDTO,
        principal: Principal?
    ): ResponseEntity<ValmistumispyynnonTarkistusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val tarkistus =
            valmistumispyyntoService.updateTarkistusByVirkailijaUserId(
                id,
                user.id!!,
                valmistumispyynnonTarkistusDTO
            )

        return ResponseEntity.ok(tarkistus)
    }
}
