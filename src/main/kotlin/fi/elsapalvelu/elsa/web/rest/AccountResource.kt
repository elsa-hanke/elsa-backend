package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class AccountResource(private val userService: UserService) {

    internal class AccountResourceException(message: String) : RuntimeException(message)

    @GetMapping("/kayttaja")
    fun getAccount(principal: Principal?): UserDTO =
        if (principal is AbstractAuthenticationToken) {
            userService.getUserFromAuthentication(principal)
        } else {
            throw AccountResourceException("Käyttäjää ei löydetty")
        }

    companion object {
        private const val serialVersionUID = 1L
    }
}
