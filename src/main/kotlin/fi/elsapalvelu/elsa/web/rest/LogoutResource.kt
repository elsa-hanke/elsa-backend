package fi.elsapalvelu.elsa.web.rest

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 * REST controller for managing global OIDC logout.
 */
@RestController
class LogoutResource {

    /**
     * `POST  /api/logout` : logout the current user.
     *
     * @param request the [HttpServletRequest].
     * @param idToken the ID token.
     * @return the [ResponseEntity] with status `200 (OK)` and a body with a global logout URL and ID token.
     */
    @PostMapping("/api/logout25")
    fun logout(
        request: HttpServletRequest,
        @AuthenticationPrincipal(expression = "idToken") idToken: Saml2AuthenticationToken?
    ): ResponseEntity<*> {
        val logoutUrl = "asfd"

        val logoutDetails = mutableMapOf(
            "logoutUrl" to logoutUrl,
            "idToken" to idToken
        )
        request.session.invalidate()
        return ResponseEntity.ok().body(logoutDetails)
    }
}
