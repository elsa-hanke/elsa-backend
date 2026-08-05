package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import jakarta.validation.Valid

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
    ): ResponseEntity<Void> {
        ilmoitusService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
