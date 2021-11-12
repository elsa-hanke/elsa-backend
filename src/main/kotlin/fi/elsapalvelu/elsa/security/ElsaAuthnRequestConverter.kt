package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.joda.time.DateTime
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder
import org.opensaml.saml.saml2.core.impl.IssuerBuilder
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext
import org.springframework.stereotype.Component
import java.util.*

@Component
class ElsaAuthnRequestConverter(private val applicationProperties: ApplicationProperties) :
    Converter<Saml2AuthenticationRequestContext, AuthnRequest> {

    private val requestBuilder: AuthnRequestBuilder = AuthnRequestBuilder()
    private val issuerBuilder: IssuerBuilder = IssuerBuilder()

    override fun convert(context: Saml2AuthenticationRequestContext): AuthnRequest {
        val issuer = issuerBuilder.buildObject()
        issuer.value =
            if (context.issuer.contains("haka")) "https://api.sandbox.elsapalvelu.fi/saml2/service-provider-metadata/haka" else context.issuer

        val authnRequest = requestBuilder.buildObject()
        authnRequest.issuer = issuer
        authnRequest.destination = context.destination
        authnRequest.assertionConsumerServiceURL = context.assertionConsumerServiceUrl.replace(
            "http",
            applicationProperties.getSecurity().samlScheme!!
        )
        authnRequest.issueInstant = DateTime.now()
        authnRequest.protocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
        authnRequest.id = "AR" + UUID.randomUUID()
        return authnRequest
    }

}
