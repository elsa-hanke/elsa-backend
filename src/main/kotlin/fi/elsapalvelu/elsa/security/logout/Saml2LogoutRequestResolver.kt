package fi.elsapalvelu.elsa.security.logout

import org.springframework.security.core.Authentication

import javax.servlet.http.HttpServletRequest

interface Saml2LogoutRequestResolver {
    /**
     * Prepare to create, sign, and serialize a SAML 2.0 Logout Request.
     *
     * By default, includes a `NameID` based on the [Authentication] instance.
     * @param request the HTTP request
     * @param authentication the current principal details
     * @return a builder, useful for overriding any aspects of the SAML 2.0 Logout Request
     * that the resolver supplied
     */
    fun resolveLogoutRequest(
        request: HttpServletRequest?,
        authentication: Authentication?
    ): Saml2LogoutRequestBuilder<*>?

    /**
     * A partial application, useful for overriding any aspects of the SAML 2.0 Logout
     * Request that the resolver supplied.
     *
     * The request returned from the [.logoutRequest] method is signed and
     * serialized
     */
    interface Saml2LogoutRequestBuilder<P : Saml2LogoutRequestBuilder<P>?> {
        /**
         * Use the given name in the SAML 2.0 Logout Request
         * @param name the name to use
         * @return the [Saml2LogoutRequestBuilder] for further customizations
         */
        fun name(name: String?): P

        /**
         * Use this relay state when sending the logout response
         * @param relayState the relay state to use
         * @return the [Saml2LogoutRequestBuilder] for further customizations
         */
        fun relayState(relayState: String?): P

        /**
         * Return a signed and serialized SAML 2.0 Logout Request and associated signed
         * request parameters
         * @return a signed and serialized SAML 2.0 Logout Request
         */
        fun logoutRequest(): Saml2LogoutRequest?
    }
}
