package fi.elsapalvelu.elsa.security

import org.apache.commons.logging.LogFactory
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class ElsaSaml2AuthenticationRequestContextResolver(private val relyingPartyRegistrationResolver: ElsaRelyingPartyRegistrationResolver? = null) :
    Saml2AuthenticationRequestContextResolver {

    private val logger = LogFactory.getLog(javaClass)

    override fun resolve(request: HttpServletRequest?): Saml2AuthenticationRequestContext? {
        val relyingParty = relyingPartyRegistrationResolver!!.convert(
            request!!
        ) ?: return null
        if (this.logger.isDebugEnabled) {
            this.logger.debug(
                "Creating SAML 2.0 Authentication Request for Asserting Party ["
                    + relyingParty.registrationId + "]"
            )
        }
        return createRedirectAuthenticationRequestContext(request, relyingParty)
    }

    private fun createRedirectAuthenticationRequestContext(
        request: HttpServletRequest,
        relyingParty: RelyingPartyRegistration
    ): Saml2AuthenticationRequestContext? {
        return Saml2AuthenticationRequestContext.builder().issuer(relyingParty.entityId)
            .relyingPartyRegistration(relyingParty)
            .assertionConsumerServiceUrl(relyingParty.assertionConsumerServiceLocation)
            .relayState(request.getParameter("RelayState")).build()
    }
}
