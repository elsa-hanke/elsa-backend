package fi.elsapalvelu.elsa.security.logout

import org.springframework.security.crypto.codec.Utf8
import org.springframework.util.Assert
import java.security.MessageDigest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


private val DEFAULT_LOGOUT_REQUEST_ATTR_NAME = (HttpSessionLogoutRequestRepository::class.java.name
    + ".LOGOUT_REQUEST")

class HttpSessionLogoutRequestRepository : Saml2LogoutRequestRepository {

    override fun loadLogoutRequest(request: HttpServletRequest?): Saml2LogoutRequest? {
        Assert.notNull(request, "request cannot be null")
        val session = request!!.getSession(false) ?: return null
        val logoutRequest =
            session.getAttribute(DEFAULT_LOGOUT_REQUEST_ATTR_NAME) as Saml2LogoutRequest
        return if (stateParameterEquals(request, logoutRequest)) {
            logoutRequest
        } else null
    }

    override fun saveLogoutRequest(
        logoutRequest: Saml2LogoutRequest?,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ) {
        Assert.notNull(request, "request cannot be null")
        Assert.notNull(response, "response cannot be null")
        if (logoutRequest == null) {
            request!!.session.removeAttribute(DEFAULT_LOGOUT_REQUEST_ATTR_NAME)
            return
        }
        val state = logoutRequest.getRelayState()
        Assert.hasText(state, "logoutRequest.state cannot be empty")
        request!!.session.setAttribute(DEFAULT_LOGOUT_REQUEST_ATTR_NAME, logoutRequest)
    }

    override fun removeLogoutRequest(
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ): Saml2LogoutRequest? {
        Assert.notNull(request, "request cannot be null")
        Assert.notNull(response, "response cannot be null")
        val logoutRequest = loadLogoutRequest(request) ?: return null
        request!!.session.removeAttribute(DEFAULT_LOGOUT_REQUEST_ATTR_NAME)
        return logoutRequest
    }


    private fun getStateParameter(request: HttpServletRequest): String? {
        return request.getParameter("RelayState")
    }

    private fun stateParameterEquals(
        request: HttpServletRequest,
        logoutRequest: Saml2LogoutRequest?
    ): Boolean {
        val stateParameter = getStateParameter(request)
        if (stateParameter == null || logoutRequest == null) {
            return false
        }
        val relayState: String? = logoutRequest.getRelayState()
        return MessageDigest.isEqual(Utf8.encode(stateParameter), Utf8.encode(relayState))
    }
}
