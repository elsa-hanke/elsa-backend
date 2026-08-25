package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.security.KOULUTTAJA
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
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.service.impl.kayttaja.UserServiceImpl
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.persistence.EntityExistsException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.util.*

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
        return erikoistuvaLaakariService.findOneByKayttajaUserIdWithValidOpintooikeudet(user.id.required())?.let {
            if (user.authorities?.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) == true ||
                user.authorities?.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA) == true
            ) {
                val samlPrincipal =
                    (principal as Saml2Authentication).principal as Saml2AuthenticatedPrincipal
                val opintooikeusId = samlPrincipal.getFirstAttribute("opintooikeusId") as Long
                val opintooikeus = it.opintooikeudet?.find { oikeus -> oikeus.id == opintooikeusId }
                it.opintooikeusKaytossaId = opintooikeus?.id
                it.erikoisalaNimi = opintooikeus?.erikoisalaNimi
                it.yliopisto = opintooikeus?.yliopistoNimi
                it.yliopistoId = opintooikeus?.id.toString()
                it.muokkausoikeudetVirkailijoilla = opintooikeus?.muokkausoikeudetVirkailijoilla
            } else {
                it.muokkausoikeudetVirkailijoilla =
                    it.opintooikeudet?.firstOrNull { o -> o.id == it.opintooikeusKaytossaId }
                        ?.muokkausoikeudetVirkailijoilla
            }
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PutMapping("")
    fun updateErikoistuvaLaakari(
        @Valid @ModelAttribute omatTiedotDTO: OmatTiedotDTO,
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        @RequestParam(required = false) laakarikoulutusSuoritettuSuomiTaiBelgia: Boolean?,
        @RequestParam(required = false) laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: Boolean?,
        principal: Principal?
    ): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id.required()
        val email = omatTiedotDTO.email.required().lowercase()

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
            user.id.required(),
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        erikoistuvaLaakariService.updateLaakarikoulutusSuoritettuSuomiTaiBelgia(
            user.id.required(), laakarikoulutusSuoritettuSuomiTaiBelgia, laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia
        )

        return userService.updateUserDetails(omatTiedotDTO, userId)
    }

    @GetMapping("/laillistamispaiva")
    fun getLaillistamispaiva(
        principal: Principal?
    ): ResponseEntity<LaillistamispaivaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return erikoistuvaLaakariService.getLaillistamispaiva(user.id.required())?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/lahikouluttajat")
    fun createLahikouluttaja(
        @Valid @RequestBody uusiLahikouluttajaDTO: UusiLahikouluttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.findOneByKayttajaUserId(user.id.required())
            ?: throw BadRequestAlertException(
                "Uuden lahikouluttajan voi lisätä vain erikoistuva lääkäri",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.uuden-lahikouluttajan-voi-lisata-vain-erikoistuva-laakari"
            )
        val kouluttajaEmail = requireNotNull(uusiLahikouluttajaDTO.sahkoposti)
        val existingKouluttaja = try {
            kayttajaService.updateKouluttajaYliopistoAndErikoisalaByEmail(
                user.id.required(),
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
                    user.id.required(),
                    KayttajaDTO(
                        etunimi = uusiLahikouluttajaDTO.etunimi,
                        sukunimi = uusiLahikouluttajaDTO.sukunimi,
                        tila = KayttajatilinTila.KUTSUTTU
                    ),
                    UserDTO(
                        login = uusiLahikouluttajaDTO.sahkoposti,
                        email = uusiLahikouluttajaDTO.sahkoposti?.lowercase(),
                        activated = true,
                        authorities = setOf(KOULUTTAJA)
                    )
                )
        val token = verificationTokenService.save(result.userId.required())
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
    }

    @PatchMapping("opinto-oikeus/{id}")
    fun updateOpintooikeusKaytossa(
        @PathVariable(value = "id", required = true) id: Long,
        principal: Principal?
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)
        opintooikeusService.setOpintooikeusKaytossa(user.id.required(), id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/muokkausoikeudet")
    fun updateMuokkausoikeudet(
        principal: Principal?,
        @RequestParam muokkausoikeudet: Boolean
    ): ResponseEntity<Unit> {
        val user = userService.getAuthenticatedUser(principal)
        opintooikeusService.updateMuokkausoikeudet(user.id.required(), muokkausoikeudet)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/kouluttajat")
    fun getKouluttajat(
        principal: Principal?
    ): ResponseEntity<List<KayttajaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(kayttajaService.findKouluttajatFromSameErikoisala(user.id.required()))
    }

    @GetMapping("/kouluttajat-vastuuhenkilot")
    fun getKouluttajatJaVastuuhenkilot(
        principal: Principal?
    ): ResponseEntity<List<KayttajaDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            kayttajaService.findKouluttajatAndVastuuhenkilotFromSameYliopisto(
                user.id.required()
            )
        )
    }
}
