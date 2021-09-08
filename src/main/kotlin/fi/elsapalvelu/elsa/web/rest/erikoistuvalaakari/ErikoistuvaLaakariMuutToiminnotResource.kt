package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KayttooikeusHakemusDTO
import fi.elsapalvelu.elsa.service.dto.UusiLahikouluttajaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariMuutToiminnotResource(
    private val kayttajaService: KayttajaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val verificationTokenService: VerificationTokenService,
    private val mailService: MailService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("")
    fun getErikoistuvaLaakari(
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        erikoistuvaLaakariService.findOneByKayttajaId(kayttaja.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("/kayttooikeushakemus")
    fun updateErikoistuvalaakariKayttooikeushakemus(
        @Valid @RequestBody kayttooikeusHakemusDTO: KayttooikeusHakemusDTO,
        principal: Principal?,
        request: HttpServletRequest
    ) {
        kayttajaService.updateKayttajaAuthorities(principal, kayttooikeusHakemusDTO)
    }

    @PostMapping("/lahikouluttajat")
    fun createLahikouluttaja(
        @Valid @RequestBody uusiLahikouluttajaDTO: UusiLahikouluttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajaDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        erikoistuvaLaakariService.findOneByKayttajaId(kayttaja.id!!)
            ?.let { _ ->
                if (kayttajaService.existsByEmail(uusiLahikouluttajaDTO.sahkoposti!!)) {
                    throw BadRequestAlertException(
                        "Samalla sähköpostilla löytyy jo käyttäjä",
                        "kayttaja",
                        "dataillegal"
                    )
                }

                val result = kayttajaService.save(
                    KayttajaDTO(
                        etunimi = uusiLahikouluttajaDTO.etunimi,
                        sukunimi = uusiLahikouluttajaDTO.sukunimi,
                        sahkopostiosoite = uusiLahikouluttajaDTO.sahkoposti,
                        authorities = setOf(KOULUTTAJA)
                    ),
                )

                val token = verificationTokenService.save(result.id!!)
                mailService.sendEmailFromTemplate(
                    Kayttaja(sahkopostiosoite = uusiLahikouluttajaDTO.sahkoposti),
                    "uusiKouluttaja.html",
                    "email.uusikouluttaja.title",
                    properties = mapOf(
                        Pair(MailProperty.ID, token),
                        Pair(MailProperty.NAME, "${kayttaja.etunimi} ${kayttaja.sukunimi}")
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
