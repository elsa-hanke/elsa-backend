package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import jakarta.validation.Valid

private const val ENTITY_NAME = "kouluttajavaltuutus"
private const val KAYTTAJA_ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKouluttajavaltuutusResource(
    private val userService: UserService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/kouluttajavaltuutukset")
    fun getKouluttajavaltuutukset(
        principal: Principal?
    ): ResponseEntity<List<KouluttajavaltuutusDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        kouluttajavaltuutusService.findAllValtuutettuByValtuuttajaKayttajaUserId(user.id!!).let {
            return ResponseEntity.ok(it)
        }
    }

    @PostMapping("/kouluttajavaltuutus")
    fun createKouluttajavaltuutus(
        @Valid @RequestBody valtuutettu: KayttajaDTO,
        principal: Principal?
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (valtuutettu.userId == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                KAYTTAJA_ENTITY_NAME,
                "idnull"
            )
        }

        kouluttajavaltuutusService.findValtuutettuByValtuuttajaAndValtuutettu(
            user.id!!,
            valtuutettu.userId!!
        ).ifPresent {
            throw BadRequestAlertException(
                "Erikoistuva on jo valtuuttanut kouluttajan",
                ENTITY_NAME,
                "dataillegal.erikoistuva-on-jo-valtuuttanut-kouluttajan"
            )
        }
        val result = kouluttajavaltuutusService.save(
            user.id!!,
            KouluttajavaltuutusDTO(
                alkamispaiva = LocalDate.now(),
                paattymispaiva = LocalDate.now().plusMonths(6),
                valtuutettu = valtuutettu
            ), true
        )

        return ResponseEntity
            .created(URI("/api/kouluttajavaltuutukset"))
            .body(result)
    }

    @PutMapping("/kouluttajavaltuutus/{id}")
    fun updateKouluttajavaltuutus(
        @PathVariable id: Long,
        @Valid @RequestBody kouluttajavaltuutusDTO: KouluttajavaltuutusDTO,
        principal: Principal?
    ): ResponseEntity<KouluttajavaltuutusDTO> {
        val user = userService.getAuthenticatedUser(principal)

        kouluttajavaltuutusService.save(
            user.id!!,
            KouluttajavaltuutusDTO(id = id, paattymispaiva = kouluttajavaltuutusDTO.paattymispaiva)
        ).let {
            return ResponseEntity.ok(it)
        }
    }
}
