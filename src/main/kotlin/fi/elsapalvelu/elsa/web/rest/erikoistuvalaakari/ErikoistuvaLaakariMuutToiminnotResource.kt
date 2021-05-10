package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariMuutToiminnotResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("")
    fun getErikoistuvaLaakari(
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("/kayttooikeushakemus")
    fun updateErikoistuvalaakariKayttooikeushakemus(
        @Valid @RequestBody kayttooikeusHakemusDTO: KayttooikeusHakemusDTO,
        principal: Principal?,
        request: HttpServletRequest
    ) {
        userService.updateUserAuthorities(principal, request)
    }

    @PostMapping("/lahikouluttajat")
    fun createLahikouluttaja(
        @Valid @RequestBody uusiLahikouluttajaDTO: UusiLahikouluttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
            ?.let { _ ->
                val result = kayttajaService.save(
                    KayttajaDTO(nimi = uusiLahikouluttajaDTO.nimi),
                    UserDTO(
                        id = UUID.randomUUID().toString(),
                        login = uusiLahikouluttajaDTO.sahkoposti,
                        email = uusiLahikouluttajaDTO.sahkoposti,
                        activated = false,
                        authorities = setOf(KOULUTTAJA)
                    )
                )

                return ResponseEntity.created(URI("/api/kayttajat/${result.id}"))
                    .headers(
                        HeaderUtil.createEntityCreationAlert(
                            applicationName,
                            true,
                            "kayttaja",
                            result.id.toString()
                        )
                    )
                    .body(result)
            } ?: throw BadRequestAlertException(
            "Uuden lahikouluttajan voi lisätä vain erikoistuva lääkäri.",
            "kayttaja",
            "dataillegal"
        )
    }
}
