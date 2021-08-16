package fi.elsapalvelu.elsa.security.logout

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


interface Saml2LogoutRequestRepository {

    /**
     * Returns the [Saml2LogoutRequest] associated to the provided
     * `HttpServletRequest` or `null` if not available.
     * @param request the `HttpServletRequest`
     * @return the [Saml2LogoutRequest] or `null` if not available
     */
    fun loadLogoutRequest(request: HttpServletRequest?): Saml2LogoutRequest?

    /**
     * Persists the [Saml2LogoutRequest] associating it to the provided
     * `HttpServletRequest` and/or `HttpServletResponse`.
     * @param logoutRequest the [Saml2LogoutRequest]
     * @param request the `HttpServletRequest`
     * @param response the `HttpServletResponse`
     */
    fun saveLogoutRequest(
        logoutRequest: Saml2LogoutRequest?,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    )

    /**
     * Removes and returns the [Saml2LogoutRequest] associated to the provided
     * `HttpServletRequest` and `HttpServletResponse` or if not available
     * returns `null`.
     * @param request the `HttpServletRequest`
     * @param response the `HttpServletResponse`
     * @return the [Saml2LogoutRequest] or `null` if not available
     */
    fun removeLogoutRequest(
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ): Saml2LogoutRequest?
}
