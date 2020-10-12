package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
class AccountResource(private val userService: UserService) {

    internal class AccountResourceException(message: String) : RuntimeException(message)

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * `GET  /account` : get the current user.
     *
     * @param principal the current user; resolves to `null` if not authenticated.
     * @return the current user.
     * @throws AccountResourceException `500 (Internal Server Error)` if the user couldn't be returned.
     */
    @GetMapping("/account")
    fun getAccount(principal: Principal?): UserDTO =
        if (principal is AbstractAuthenticationToken) {
            userService.getUserFromAuthentication(principal)
        } else {
            throw AccountResourceException("User could not be found")
        }

    companion object {
        private const val serialVersionUID = 1L
    }
}
