package fi.elsapalvelu.elsa.web.rest

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.security.Principal
import jakarta.validation.Valid

private const val KAYTTAJA_ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val yliopistoService: YliopistoService,
    private val erikoisalaService: ErikoisalaService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/kayttaja")
    fun getKayttaja(principal: Principal?): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val user = userService.getUser(userId)
        val authorities =
            (principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)

        user.impersonated =
            authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) || authorities.contains(
                ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
            )
        return user
    }

    @GetMapping("/kayttaja-lisatiedot")
    fun getKayttajaLisatiedot(principal: Principal?): KayttajaTiedotDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val kayttaja = kayttajaService.findByUserId(userId).orElse(null)
        return KayttajaTiedotDTO(
            nimike = kayttaja?.nimike,
            kayttajanYliopistot = kayttaja?.yliopistot,
            kayttajanYliopistotJaErikoisalat = kayttaja?.yliopistotAndErikoisalat?.groupBy { it.yliopisto }
                ?.map {
                    KayttajaYliopistoErikoisalatDTO(
                        it.key,
                        it.value.filter { kye -> kye.erikoisala != null }
                            .map { kye -> kye.erikoisala!! }
                    )
                }?.toMutableSet(),
            yliopistot = yliopistoService.findAll(),
            erikoisalat = erikoisalaService.findAllByLiittynytElsaan()
        )
    }

    @GetMapping("/kayttaja-impersonated")
    fun getKayttajaImpersonated(principal: Principal?): UserDTO {
        (principal as Saml2Authentication).authorities.forEach {
            if (it is SwitchUserGrantedAuthority) {
                return userService.getUser(it.source.name)
            }
        }
        return getKayttaja(principal)
    }

    @PutMapping("/kayttaja")
    fun updateKayttajaDetails(
        @Valid @ModelAttribute omatTiedotDTO: OmatTiedotDTO,
        @Valid @RequestParam kayttajanYliopistotJaErikoisalat: String?,
        principal: Principal?
    ): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val user = userService.getUser(userId)
        val email = omatTiedotDTO.email!!.lowercase()

        val userDTO = userService.getUser(userId)
        if (userDTO.email?.lowercase() != email && userService.existsByEmail(email)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        val kayttajanYliopistotDTO: List<KayttajaYliopistoErikoisalatDTO> =
            kayttajanYliopistotJaErikoisalat?.let {
                objectMapper.readValue(
                    kayttajanYliopistotJaErikoisalat,
                    objectMapper.typeFactory.constructCollectionType(
                        List::class.java,
                        KayttajaYliopistoErikoisalatDTO::class.java
                    )
                )
            } ?: listOf()

        if (user.authorities?.contains(KOULUTTAJA) == true) {
            kayttajaService.updateKayttaja(
                userId,
                omatTiedotDTO.nimike,
                kayttajanYliopistotDTO,
                true
            )
        } else if (user.authorities?.contains(VASTUUHENKILO) == true) {
            kayttajaService.updateKayttaja(
                userId,
                omatTiedotDTO.nimike,
                kayttajanYliopistotDTO,
                false
            )
        }

        return userService.updateUserDetails(omatTiedotDTO, userId)
    }

    @PostMapping("/vaihda-rooli")
    fun vaihdaRooli(
        @Valid @RequestParam rooli: String,
        principal: Principal?
    ): ResponseEntity<Void> {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val user = userService.getUser(userId)

        if (user.authorities!!.size < 2) {
            throw BadRequestAlertException(
                "Käyttäjällä ei ole useita rooleja.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.kayttajalla-ei-ole-useita-rooleja"
            )
        }

        if (!user.authorities!!.contains(rooli)) {
            throw BadRequestAlertException(
                "Käyttäjällä ei ole haluttua roolia.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.kayttajalla-ei-ole-haluttua-roolia"
            )
        }

        userService.updateRooli(rooli, userId)
        return ResponseEntity.noContent().build()
    }
}
