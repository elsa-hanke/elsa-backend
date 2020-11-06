package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.AccountResource
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.PaginationUtil
import io.github.jhipster.web.util.ResponseUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.security.Principal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariToiminnotResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val epaOsaamisalueService: EpaOsaamisalueService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {
    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("")
    fun getErikoistuvaLaakari(
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val user = getAuthenticatedUser(principal)
        return ResponseUtil.wrapOrNotFound(erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!))
    }

    @GetMapping("/suoritusarvioinnit-rajaimet")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinnitOptionsDto> {
        val user = getAuthenticatedUser(principal)
        val id = user.id!!
        val options = SuoritusarvioinnitOptionsDto()
        options.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        options.epaOsaamisalueet = epaOsaamisalueService.findAll().toMutableSet()
        options.tapahtumat = suoritusarviointiService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        options.kouluttajat = kouluttajavaltuutusService
            .findAllValtuutettuByValtuuttajaKayttajaUserId(id).toMutableSet()

        return ResponseEntity.ok(options)
    }

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        criteria: SuoritusarviointiCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<SuoritusarviointiDTO>> {
        val user = getAuthenticatedUser(principal)
        val id = user.id!!
        val page = suoritusarviointiQueryService
            .findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(criteria, id, pageable)
        val headers = PaginationUtil
            .generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)

        return ResponseEntity.ok().headers(headers).body(page)
    }

    @GetMapping("/arviointipyynto-lomake")
    fun getSuoritusarviointiPyyntolomake(
        principal: Principal?
    ): ResponseEntity<ArviointipyyntoFormDTO> {
        val user = getAuthenticatedUser(principal)
        val id = user.id!!
        val form = ArviointipyyntoFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        form.epaOsaamisalueet = epaOsaamisalueService.findAll().toMutableSet()
        form.kouluttajat = kouluttajavaltuutusService.findAllValtuutettuByValtuuttajaKayttajaUserId(id).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/suoritusarvioinnit/arviointipyynto")
    fun createArviointipyynto(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = getAuthenticatedUser(principal)
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää ID:tä.",
                "suoritusarviointi", "idexists"
            )
        }
        if (suoritusarviointiDTO.vaativuustaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää vaativuustasoa. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        if (suoritusarviointiDTO.sanallinenArviointi != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää sanallista arviointi. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        if (suoritusarviointiDTO.arviointiAika != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arvioinnin aikaa. Kouluttaja määrittelee sen.",
                "suoritusarviointi", "dataillegal"
            )
        }
        if (suoritusarviointiDTO.tyoskentelyjaksoId == null) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                "suoritusarviointi", "dataillegal"
            )
        } else {
            val tyoskentelyjakso = tyoskentelyjaksoService.findOne(suoritusarviointiDTO.tyoskentelyjaksoId!!)
            val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
            if (!tyoskentelyjakso.isPresent || !erikoistuvaLaakari.isPresent) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    "suoritusarviointi", "dataillegal"
                )
            }
            if (tyoskentelyjakso.get().erikoistuvaLaakariId!! != erikoistuvaLaakari.get().id) {
                throw BadRequestAlertException(
                    "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                    "suoritusarviointi", "dataillegal"
                )
            }
        }
        suoritusarviointiDTO.pyynnonAika = LocalDate.now(ZoneId.systemDefault())

        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
            .headers(HeaderUtil.createEntityCreationAlert(
                applicationName,
                true,
                "suoritusarviointi",
                result.id.toString())
            )
            .body(result)
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = getAuthenticatedUser(principal)
        val suoritusarviointiDTO = suoritusarviointiService
            .findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjakso: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val tyoskentelypaikka = tyoskentelyjakso.tyoskentelypaikka
        if (tyoskentelyjakso.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä.",
                "tyoskentelyjakso",
                "idexists"
            )
        }
        if (tyoskentelypaikka == null || tyoskentelypaikka.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä.",
                "tyoskentelypaikka",
                "idexists"
            )
        }
        val user = getAuthenticatedUser(principal)
        val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
        if (erikoistuvaLaakari.isPresent) {
            tyoskentelyjakso.erikoistuvaLaakariId = erikoistuvaLaakari.get().id

            val result = tyoskentelyjaksoService.save(tyoskentelyjakso)
            return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
                .headers(HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    "tyoskentelyjakso",
                    result.id.toString())
                )
                .body(result)
        } else {
            throw BadRequestAlertException(
                "Uuden tyoskentelyjakson voi tehdä vain erikoistuva lääkäri.",
                "tyoskentelyjakso",
                "dataillegal"
            )
        }
    }

    @PostMapping("/lahikouluttajat")
    fun createLahikouluttaja(
        @Valid @RequestBody uusiLahikouluttaja: UusiLahikouluttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajaDTO> {
        val user = getAuthenticatedUser(principal)
        val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
        if (erikoistuvaLaakari.isPresent) {
            val result = kayttajaService.save(
                KayttajaDTO(nimi = uusiLahikouluttaja.nimi),
                UserDTO(
                    id = UUID.randomUUID().toString(),
                    login = uusiLahikouluttaja.sahkoposti,
                    email = uusiLahikouluttaja.sahkoposti,
                    activated = false,
                )
            )
            val kouluttajavaltuutus = KouluttajavaltuutusDTO(
                alkamispaiva = LocalDate.now(ZoneId.systemDefault()),
                paattymispaiva = LocalDate.now(ZoneId.systemDefault()).plusMonths(6),
                valtuutuksenLuontiaika = Instant.now(),
                valtuutuksenMuokkausaika = Instant.now(),
                valtuuttajaId = erikoistuvaLaakari.get().id,
                valtuutettuId = result.id,
            )

            kouluttajavaltuutusService.save(kouluttajavaltuutus)
            return ResponseEntity.created(URI("/api/kayttajat/${result.id}"))
                .headers(HeaderUtil.createEntityCreationAlert(
                    applicationName,
                    true,
                    "kayttaja",
                    result.id.toString())
                )
                .body(result)
        } else {
            throw BadRequestAlertException(
                "Uuden lahikouluttajan voi lisätä vain erikoistuva lääkäri.",
                "kayttaja",
                "dataillegal"
            )
        }
    }

    fun getAuthenticatedUser(principal: Principal?): UserDTO {
        if (principal is AbstractAuthenticationToken) {
            return userService.getUserFromAuthentication(principal)
        } else {
            throw AccountResource.AccountResourceException("Käyttäjä ei löydy")
        }
    }
}
