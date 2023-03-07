package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.impl.UserServiceImpl
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import java.util.*
import jakarta.persistence.EntityExistsException
import jakarta.validation.Valid

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
        erikoistuvaLaakariService.findOneByKayttajaUserIdWithValidOpintooikeudet(user.id!!)?.let {
            if (user.authorities?.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) == true ||
                user.authorities?.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA) == true
            ) {
                val samlPrincipal =
                    (principal as Saml2Authentication).principal as Saml2AuthenticatedPrincipal
                val opintooikeusId = samlPrincipal.getFirstAttribute("opintooikeusId") as Long
                val opintooikeus = it.opintooikeudet?.find { oikeus -> oikeus.id == opintooikeusId }
                it.erikoisalaNimi = opintooikeus?.erikoisalaNimi
                it.yliopisto = opintooikeus?.yliopistoNimi
                it.yliopistoId = opintooikeus?.id.toString()
                it.muokkausoikeudetVirkailijoilla = opintooikeus?.muokkausoikeudetVirkailijoilla
            } else {
                it.muokkausoikeudetVirkailijoilla =
                    it.opintooikeudet?.first { o -> o.id == it.opintooikeusKaytossaId }?.muokkausoikeudetVirkailijoilla
            }
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("")
    fun updateErikoistuvaLaakari(
        @Valid @ModelAttribute omatTiedotDTO: OmatTiedotDTO,
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        principal: Principal?
    ): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val email = omatTiedotDTO.email!!.lowercase()

        val userDTO = userService.getUser(userId)
        if (userDTO.email?.lowercase() != email && userService.existsByEmail(email)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        return userService.updateUserDetails(omatTiedotDTO, userId)
    }

    @GetMapping("/laillistamispaiva")
    fun getLaillistamispaiva(
        principal: Principal?
    ): ResponseEntity<LaillistamispaivaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.getLaillistamispaiva(user.id!!)?.let {
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
                    KayttajaDTO(
                        etunimi = uusiLahikouluttajaDTO.etunimi,
                        sukunimi = uusiLahikouluttajaDTO.sukunimi,
                        tila = KayttajatilinTila.KUTSUTTU
                    ),
                    UserDTO(
                        id = UUID.randomUUID().toString(),
                        login = uusiLahikouluttajaDTO.sahkoposti,
                        email = uusiLahikouluttajaDTO.sahkoposti?.lowercase(),
                        activated = true,
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

    @PostMapping("/muokkausoikeudet")
    fun updateMuokkausoikeudet(
        principal: Principal?,
        @RequestParam muokkausoikeudet: Boolean
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        opintooikeusService.updateMuokkausoikeudet(user.id!!, muokkausoikeudet)
        return ResponseEntity.ok().build()
    }
}
