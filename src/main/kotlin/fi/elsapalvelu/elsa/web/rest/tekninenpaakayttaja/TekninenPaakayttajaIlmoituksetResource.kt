package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaIlmoituksetResource(
    private val ilmoitusService: IlmoitusService
) {
    @PostMapping("/ilmoitukset")
    fun createIlmoitus(@Valid @RequestBody ilmoitusDTO: IlmoitusDTO): ResponseEntity<IlmoitusDTO> {
        return ResponseEntity.ok(ilmoitusService.create(ilmoitusDTO))
    }

    @PutMapping("/ilmoitukset")
    fun updateIlmoitus(@Valid @RequestBody ilmoitusDTO: IlmoitusDTO): ResponseEntity<IlmoitusDTO> {
        return ResponseEntity.ok(ilmoitusService.update(ilmoitusDTO))
    }

    @DeleteMapping("/ilmoitukset/{id}")
    fun deleteTeoriakoulutus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        ilmoitusService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
