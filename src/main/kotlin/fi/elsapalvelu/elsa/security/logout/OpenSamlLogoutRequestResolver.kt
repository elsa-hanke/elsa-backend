package fi.elsapalvelu.elsa.security.logout

import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.joda.time.DateTime
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.core.xml.io.MarshallingException
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.LogoutRequest
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.impl.IssuerBuilder
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder
import org.opensaml.saml.saml2.core.impl.LogoutRequestMarshaller
import org.opensaml.saml.saml2.core.impl.NameIDBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.saml2.Saml2Exception
import org.springframework.security.saml2.core.OpenSamlInitializationService
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.security.web.util.UrlUtils
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest

@Component
class OpenSamlLogoutRequestResolver(private val relyingPartyRegistrationRepository: RelyingPartyRegistrationRepository) :
    Saml2LogoutRequestResolver {

    /**
     * Prepare to create, sign, and serialize a SAML 2.0 Logout Request.
     *
     * By default, includes a {@code NameID} based on the {@link Authentication} instance
     * as well as the {@code Destination} and {@code Issuer} based on the
     * {@link RelyingPartyRegistration} derived from the {@link Authentication}.
     *
     * The {@link Authentication} must be of type {@link Saml2Authentication} in order to
     * look up the {@link RelyingPartyRegistration} that holds the signing key.
     * @param request the HTTP request
     * @param authentication the current principal details
     * @return a builder, useful for overriding any aspects of the SAML 2.0 Logout Request
     * that the resolver supplied
     */
    override fun resolveLogoutRequest(
        request: HttpServletRequest?,
        authentication: Authentication?
    ): Saml2LogoutRequestResolver.Saml2LogoutRequestBuilder<*> {
        Assert.notNull(
            authentication,
            "Failed to lookup logged-in user for formulating LogoutRequest"
        )
        val registration = relyingPartyRegistrationRepository.findByRegistrationId("suomifi")
        val uriComponents = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
            .replacePath(request?.contextPath).replaceQuery(null).fragment(null).build()
        Assert.notNull(
            registration,
            "Failed to lookup RelyingPartyRegistration for formulating LogoutRequest"
        )
        val registry = ConfigurationService.get(XMLObjectProviderRegistry::class.java)
        val marshaller =
            registry.marshallerFactory.getMarshaller(LogoutRequest.DEFAULT_ELEMENT_NAME) as LogoutRequestMarshaller
        val issuerBuilder =
            registry.builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME) as IssuerBuilder
        val nameIdBuilder =
            registry.builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME) as NameIDBuilder
        val logoutRequestBuilder =
            registry.builderFactory.getBuilder(LogoutRequest.DEFAULT_ELEMENT_NAME) as LogoutRequestBuilder
        val principal =
            (authentication as Saml2Authentication).principal as Saml2AuthenticatedPrincipal
        return OpenSamlLogoutRequestBuilder(
            registration,
            marshaller,
            issuerBuilder,
            nameIdBuilder,
            logoutRequestBuilder.buildObject(),
            UUID.randomUUID().toString()
        )
            .destination(
                registration.assertingPartyDetails.singleSignOnServiceLocation.replace(
                    "SSO",
                    "SLO"
                )
            )
            .issuer(
                registration.entityId.replace("{baseUrl}", uriComponents.toUriString())
                    .replace("{registrationId}", registration.registrationId)
            ).nameWithParameters(
                principal.getFirstAttribute("nameID"),
                principal.getFirstAttribute("nameIDFormat"),
                principal.getFirstAttribute("nameIDQualifier"),
                principal.getFirstAttribute("nameIDSPQualifier")
            )
    }

    /**
     * A builder, useful for overriding any aspects of the SAML 2.0 Logout Request that
     * the resolver supplied.
     *
     * The request returned from the {@link #logoutRequest()} method is signed and
     * serialized. It will at minimum include an {@code ID} and a {@code RelayState},
     * though note that callers should also provide an {@code IssueInstant}. For your
     * convenience, {@link OpenSamlLogoutRequestResolver} also sets some default values.
     *
     * This builder is specifically handy for getting access to the underlying
     * {@link LogoutRequest} to make changes before it gets signed and serialized
     *
     * @see OpenSamlLogoutRequestResolver#resolveLogoutRequest
     */
    class OpenSamlLogoutRequestBuilder(
        private val registration: RelyingPartyRegistration,
        private val marshaller: LogoutRequestMarshaller,
        private val issuerBuilder: IssuerBuilder,
        private val nameIDBuilder: NameIDBuilder,
        private val logoutRequest: LogoutRequest,
        private var relayState: String?
    ) : Saml2LogoutRequestResolver.Saml2LogoutRequestBuilder<OpenSamlLogoutRequestBuilder> {

        init {
            OpenSamlInitializationService.initialize()
        }

        override fun name(name: String?): OpenSamlLogoutRequestBuilder {
            val nameId = nameIDBuilder.buildObject()
            nameId.value = name
            this.logoutRequest.nameID = nameId
            return this
        }

        fun nameWithParameters(
            name: String?,
            format: String?,
            qualifier: String?,
            spQualifier: String?
        ): OpenSamlLogoutRequestBuilder {
            val nameId = nameIDBuilder.buildObject()
            nameId.value = name
            nameId.format = format
            nameId.nameQualifier = qualifier
            nameId.spNameQualifier = spQualifier
            this.logoutRequest.nameID = nameId
            return this
        }

        /**
         * {@inheritDoc}
         */
        override fun relayState(relayState: String?): OpenSamlLogoutRequestBuilder {
            this.relayState = relayState
            return this
        }

        /**
         * Mutate the {@link LogoutRequest} using the provided {@link Consumer}
         * @param request the Logout Request {@link Consumer} to use
         * @return the {@link OpenSamlLogoutRequestBuilder} for further customizations
         */
        fun logoutRequest(request: Consumer<LogoutRequest>): OpenSamlLogoutRequestBuilder {
            request.accept(this.logoutRequest)
            return this
        }

        /**
         * {@inheritDoc}
         */
        override fun logoutRequest(): Saml2LogoutRequest {
            if (this.logoutRequest.id == null) {
                this.logoutRequest.id = "LR" + UUID.randomUUID()
            }
            if (this.relayState == null) {
                this.relayState = UUID.randomUUID().toString()
            }
            val result = Saml2LogoutRequest(
                registration.assertingPartyDetails.singleSignOnServiceLocation.replace(
                    "SSO",
                    "SLO"
                ),
                registration.assertingPartyDetails.singleSignOnServiceBinding,
                mutableMapOf(),
                logoutRequest.id,
                registration.registrationId
            )
            if (this.registration.assertingPartyDetails.singleSignOnServiceBinding == Saml2MessageBinding.POST
            ) {
                val xml = serialize(
                    OpenSamlSigningUtils.sign(
                        this.logoutRequest,
                        this.registration
                    )
                )
                result.parameters["SAMLRequest"] = Saml2Utils.samlEncode(
                    xml.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )!!
                return result
            } else {
                val xml = serialize(this.logoutRequest)
                val deflatedAndEncoded = Saml2Utils.samlEncode(Saml2Utils.samlDeflate(xml))
                result.parameters["SAMLRequest"] = deflatedAndEncoded!!
                val parameters = OpenSamlSigningUtils.sign(this.registration)
                    .param("SAMLRequest", deflatedAndEncoded).param("RelayState", this.relayState)
                    .parameters()
                parameters.forEach { (t, u) -> result.parameters.putIfAbsent(t, u!!) }
                return result
            }
        }

        private fun serialize(logoutRequest: LogoutRequest): String {
            try {
                val element = this.marshaller.marshall(logoutRequest)
                return SerializeSupport.nodeToString(element)
            } catch (ex: MarshallingException) {
                throw Saml2Exception(ex)
            }
        }

        fun destination(destination: String): OpenSamlLogoutRequestBuilder {
            this.logoutRequest.destination = destination
            return this
        }

        fun issuer(issuer: String): OpenSamlLogoutRequestBuilder {
            val iss = this.issuerBuilder.buildObject()
            iss.value = issuer
            this.logoutRequest.issuer = iss
            this.logoutRequest.issueInstant = DateTime.now()
            return this
        }
    }
}
