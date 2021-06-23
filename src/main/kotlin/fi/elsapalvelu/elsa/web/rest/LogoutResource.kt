package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.security.logout.OpenSamlLogoutRequestResolver
import fi.elsapalvelu.elsa.security.logout.Saml2LogoutRequest
import fi.elsapalvelu.elsa.security.logout.Saml2LogoutRequestResolver
import org.springframework.http.ResponseEntity
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.security.Principal
import javax.servlet.http.HttpServletRequest

/**
 * REST controller for managing global OIDC logout.
 */
@RestController
class LogoutResource(private val logoutRequestResolver: OpenSamlLogoutRequestResolver) {

    /**
     * `POST  /api/logout` : logout the current user.
     *
     * @param request the [HttpServletRequest].
     * @param idToken the ID token.
     * @return the [ResponseEntity] with status `200 (OK)` and a body with a global logout URL and ID token.
     */
    @PostMapping("/api/logout")
    fun logout(
        request: HttpServletRequest,
        principal: Principal?,
    ): ResponseEntity<*> {
        val builder: Saml2LogoutRequestResolver.Saml2LogoutRequestBuilder<*> =
            this.logoutRequestResolver.resolveLogoutRequest(
                request,
                principal as Saml2Authentication
            )
        val logoutRequest = builder.logoutRequest()
        val location: String = logoutRequest!!.location
        val uriBuilder = UriComponentsBuilder.fromUriString(location)
        addParameter("SAMLRequest", logoutRequest, uriBuilder)
        addParameter("RelayState", logoutRequest, uriBuilder)
        addParameter("SigAlg", logoutRequest, uriBuilder)
        addParameter("Signature", logoutRequest, uriBuilder)
        val logoutUrl = uriBuilder.build(true).toUriString()

        val logoutDetails = mutableMapOf(
            "logoutUrl" to logoutUrl
        )
        request.session.invalidate()
        return ResponseEntity.ok().body(logoutDetails)
    }

    private fun addParameter(
        name: String,
        logoutRequest: Saml2LogoutRequest,
        builder: UriComponentsBuilder
    ) {
        Assert.hasText(name, "name cannot be empty or null")
        if (StringUtils.hasText(logoutRequest.getParameter(name))) {
            builder.queryParam(
                UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                UriUtils.encode(logoutRequest.getParameter(name)!!, StandardCharsets.ISO_8859_1)
            )
        }
    }
}
