package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.UusiLahikouluttajaDTO
import fi.elsapalvelu.elsa.service.impl.UserServiceImpl
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.util.*
import javax.persistence.EntityExistsException
import javax.validation.Valid

private const val KAYTTAJA_ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariMuutToiminnotResource(
    private val userService: UserServiceImpl,
    private val kayttajaService: KayttajaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val opintooikeusService: OpintooikeusService,
    private val verificationTokenService: VerificationTokenService,
    private val mailService: MailService
) {
    @GetMapping("")
    fun getErikoistuvaLaakari(
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/lahikouluttajat")
    fun createLahikouluttaja(
        @Valid @RequestBody uusiLahikouluttajaDTO: UusiLahikouluttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
            ?.let {
                val kouluttajaEmail = requireNotNull(uusiLahikouluttajaDTO.sahkoposti)
                val existingKouluttaja = try {
                    kayttajaService.updateKouluttajaYliopistoAndErikoisalaByEmail(
                        user.id!!,
                        kouluttajaEmail
                    )
                } catch (ex: EntityExistsException) {
                    throw BadRequestAlertException(
                        "Samalla sähköpostilla löytyy jo toinen käyttäjä saman yliopiston ja erikoisalan alta",
                        KAYTTAJA_ENTITY_NAME,
                        "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja-saman-yliopiston-ja-erikoisalan-alta"
                    )
                }

                if (existingKouluttaja != null) {
                    return ResponseEntity.ok(existingKouluttaja)
                }

                val result = kayttajaService.saveKouluttaja(
                    user.id!!,
                    KayttajaDTO(nimi = uusiLahikouluttajaDTO.nimi),
                    UserDTO(
                        id = UUID.randomUUID().toString(),
                        login = uusiLahikouluttajaDTO.sahkoposti,
                        email = uusiLahikouluttajaDTO.sahkoposti,
                        activated = false,
                        authorities = setOf(KOULUTTAJA)
                    )
                )
                val token = verificationTokenService.save(result.userId!!)
                mailService.sendEmailFromTemplate(
                    User(email = uusiLahikouluttajaDTO.sahkoposti),
                    templateName = "uusiKouluttaja.html",
                    titleKey = "email.uusikouluttaja.title",
                    properties = mapOf(
                        Pair(MailProperty.ID, token),
                        Pair(MailProperty.NAME, user.firstName + " " + user.lastName)
                    )
                )

                return ResponseEntity
                    .created(URI("/api/kayttajat/${result.id}"))
                    .body(result)
            } ?: throw BadRequestAlertException(
            "Uuden lahikouluttajan voi lisätä vain erikoistuva lääkäri",
            KAYTTAJA_ENTITY_NAME,
            "dataillegal.uuden-lahikouluttajan-voi-lisata-vain-erikoistuva-laakari"
        )
    }

    @PatchMapping("opinto-oikeus/{id}")
    fun updateOpintooikeusKaytossa(
        @PathVariable(value = "id", required = true) id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        opintooikeusService.setOpintooikeusKaytossa(user.id!!, id)
        return ResponseEntity.ok().build()
    }
}
