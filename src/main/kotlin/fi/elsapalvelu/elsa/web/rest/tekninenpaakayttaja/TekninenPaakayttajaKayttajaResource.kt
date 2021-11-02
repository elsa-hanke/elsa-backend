package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaFormDTO
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaKayttajaResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val yliopistoService: YliopistoService,
    private val erikoisalaService: ErikoisalaService
) {

    @GetMapping("/erikoistuvat-laakarit")
    @PreAuthorize("hasAuthority('ROLE_TEKNINEN_PAAKAYTTAJA')")
    fun getErikoistuvatLaakarit(principal: Principal?): ResponseEntity<List<ErikoistuvaLaakariDTO>> {
        return ResponseEntity.ok(erikoistuvaLaakariService.findAll())
    }

    @GetMapping("/kayttajat/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEKNINEN_PAAKAYTTAJA')")
    fun getKayttaja(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaDTO> {
        val kayttaja: KayttajaDTO? = kayttajaService.findOne(id).orElse(null)
        val user = kayttaja?.let { userService.getUser(it.userId!!) }
        val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaId(id)

        return ResponseEntity.ok(
            KayttajahallintaKayttajaDTO(
                user = user,
                kayttaja = kayttaja,
                erikoistuvaLaakari = erikoistuvaLaakari
            )
        )
    }

    @GetMapping("/kayttaja-lomake")
    @PreAuthorize("hasAuthority('ROLE_TEKNINEN_PAAKAYTTAJA')")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaFormDTO> {
        val form = KayttajahallintaKayttajaFormDTO()

        form.yliopistot = yliopistoService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/erikoistuvat-laakarit")
    @PreAuthorize("hasAuthority('ROLE_TEKNINEN_PAAKAYTTAJA')")
    fun createErikoistuvaLaakari(
        @Valid @RequestBody kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO,
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val result = erikoistuvaLaakariService.save(kayttajahallintaErikoistuvaLaakariDTO)

        return ResponseEntity
            .created(URI("/api/tekninen-paakayttaja/kayttajat/${result.kayttajaId}"))
            .body(result)

    }

    @PostMapping("/erikoistuvat-laakarit/{id}/invite")
    @PreAuthorize("hasAuthority('ROLE_TEKNINEN_PAAKAYTTAJA')")
    fun resendErikoistuvaLaakariInvitation(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        erikoistuvaLaakariService.resendInvitation(id)

        return ResponseEntity
            .noContent()
            .build()
    }
}
