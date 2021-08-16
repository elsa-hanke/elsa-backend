package fi.elsapalvelu.elsa.security.logout

import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import java.io.Serializable


data class Saml2LogoutRequest(
    val location: String,
    val binding: Saml2MessageBinding,
    val parameters: MutableMap<String, String>,
    val id: String,
    val relyingPartyRegistrationId: String
) : Serializable {

    fun getParameter(name: String?): String? {
        return parameters[name]
    }

    fun getRelayState(): String? {
        return parameters["RelayState"]
    }

    fun getSamlRequest(): String? {
        return parameters["SAMLRequest"]
    }

}
