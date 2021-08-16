package fi.elsapalvelu.elsa.security.logout

import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding

class Saml2LogoutResponse(
    private val location: String,
    private val binding: Saml2MessageBinding,
    private val parameters: Map<String, String>
) {
    fun getResponseLocation(): String {
        return location
    }

    fun getParameter(name: String?): String? {
        return parameters[name]
    }
}
