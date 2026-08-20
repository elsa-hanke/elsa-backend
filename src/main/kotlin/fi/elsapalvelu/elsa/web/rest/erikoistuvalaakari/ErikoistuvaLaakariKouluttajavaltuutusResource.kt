package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.time.LocalDate
import java.security.Principal
import fi.elsapalvelu.elsa.service.kayttaja.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import jakarta.validation.Valid

private const val ENTITY_NAME = "kouluttajavaltuutus"
private const val KAYTTAJA_ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKouluttajavaltuutusResource(
    private val userService: UserService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @GetMapping("/kouluttajavaltuutukset")
    fun getKouluttajavaltuutukset(
        principal: Principal?
    ): ResponseEntity<List<KouluttajavaltuutusDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        kouluttajavaltuutusService.findAllValtuutettuByValtuuttajaKayttajaUserId(user.id.required()).let {
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
            user.id.required(),
            valtuutettu.userId.required()
        ).ifPresent {
            throw BadRequestAlertException(
                "Erikoistuva on jo valtuuttanut kouluttajan",
                ENTITY_NAME,
                "dataillegal.erikoistuva-on-jo-valtuuttanut-kouluttajan"
            )
        }
        val result = kouluttajavaltuutusService.save(
            user.id.required(),
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
            user.id.required(),
            KouluttajavaltuutusDTO(id = id, paattymispaiva = kouluttajavaltuutusDTO.paattymispaiva)
        ).let {
            return ResponseEntity.ok(it)
        }
    }
}
