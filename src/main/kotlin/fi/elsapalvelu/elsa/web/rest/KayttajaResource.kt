package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
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
    fun updateKayttajaDetails(@Valid @RequestBody omatTiedotDTO: OmatTiedotDTO,
                       principal: Principal?): UserDTO {
        val user = userService.getAuthenticatedUser(principal)
        return userService.updateUserDetails(omatTiedotDTO, user.id!!)
    }
}
