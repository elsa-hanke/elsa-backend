package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val userService: UserService
) {

    @GetMapping("/kayttaja")
    fun getKayttaja(principal: Principal?): UserDTO = userService.getAuthenticatedUser(principal)

    @PutMapping("/kayttaja")
    fun updateKayttajaDetails(
        @Valid @ModelAttribute omatTiedotDTO: OmatTiedotDTO,
        principal: Principal?
    ): UserDTO {
        val user = userService.getAuthenticatedUser(principal)
        if (user.email != omatTiedotDTO.email && userService.existsByEmail(omatTiedotDTO.email)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo käyttäjä",
                "kayttaja",
                "dataillegal"
            )
        }
        
        return userService.updateUserDetails(omatTiedotDTO, user.id!!)
    }
}
