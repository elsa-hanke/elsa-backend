package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

private const val KAYTTAJA_ENTITY_NAME = "kayttaja"

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val userService: UserService,
) {

    @GetMapping("/kayttaja")
    fun getKayttaja(principal: Principal?): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val user = userService.getUser(userId)
        val authorities = (principal as Saml2Authentication).authorities.map(GrantedAuthority::getAuthority)

        user.impersonated = authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) || authorities.contains(
            ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
        )
        return user
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
        principal: Principal?
    ): UserDTO {
        val userId = userService.getAuthenticatedUser(principal).id!!
        val email = omatTiedotDTO.email!!

        val userDTO = userService.getUser(userId)
        if (userDTO.email != email && userService.existsByEmail(email)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        return userService.updateUserDetails(omatTiedotDTO, userId)
    }
}
