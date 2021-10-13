package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
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

        return userService.getUser(userId)
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
