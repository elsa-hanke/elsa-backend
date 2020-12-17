package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class KayttajaResource(
    private val userService: UserService
) {

    @GetMapping("/kayttaja")
    fun getKayttaja(principal: Principal?): UserDTO = userService.getAuthenticatedUser(principal)
}
