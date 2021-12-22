package fi.elsapalvelu.elsa.security

import org.opensaml.saml.common.xml.SAMLConstants
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder
import org.opensaml.saml.saml2.core.impl.IssuerBuilder
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class AuthnRequestConverter : Converter<Saml2AuthenticationRequestContext, AuthnRequest> {
    private val authnRequestBuilder: AuthnRequestBuilder = AuthnRequestBuilder()
    private val issuerBuilder: IssuerBuilder = IssuerBuilder()

    override fun convert(context: Saml2AuthenticationRequestContext): AuthnRequest {
        val issuer = if (context.issuer.contains("haka")) context.issuer.substring(
            0,
            context.issuer.indexOf("haka")
        ) + "haka"; else context.issuer
        val destination = context.destination
        val assertionConsumerServiceUrl = context.assertionConsumerServiceUrl
        val protocolBinding = context.relyingPartyRegistration.assertionConsumerServiceBinding.urn
        val auth = authnRequestBuilder.buildObject()
        if (auth.id == null) {
            auth.id = "ARQ" + UUID.randomUUID().toString().substring(1)
        }
        if (auth.issueInstant == null) {
            auth.issueInstant = Instant.now()
        }
        if (auth.isForceAuthn == null) {
            auth.isForceAuthn = false
        }
        if (auth.isPassive == null) {
            auth.setIsPassive(false)
        }
        if (auth.protocolBinding == null) {
            auth.protocolBinding = SAMLConstants.SAML2_POST_BINDING_URI
        }
        auth.protocolBinding = protocolBinding
        val iss = issuerBuilder.buildObject()
        iss.value = issuer
        auth.issuer = iss
        auth.destination = destination
        auth.assertionConsumerServiceURL = assertionConsumerServiceUrl
        return auth
    }
}
