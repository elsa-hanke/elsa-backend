package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.security.testSamlPrincipal
import jakarta.servlet.http.HttpServletRequest
import okhttp3.tls.HeldCertificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.saml.saml2.core.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.core.OpenSamlInitializationService
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken
import org.springframework.security.saml2.provider.service.registration.*
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.ForwardedHeaderFilter
import java.time.Instant
import java.util.Base64
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

class SamlMigrationTest {
    @BeforeEach
    fun initializeSaml() {
        OpenSamlInitializationService.initialize()
    }

    @AfterEach
    fun clearRequestContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @ParameterizedTest
    @CsvSource("haka-tampere,haka", "haka-helsinki,haka", "suomifi,suomifi")
    fun authenticationRequestUsesExternalHttpsUrlAndCorrectIssuer(registrationId: String, issuerSuffix: String) {
        val registration = registration(registrationId)
        val resolver = requireNotNull(testSecurityConfiguration().authenticationRequestResolver(
            InMemoryRelyingPartyRegistrationRepository(registration)
        ))
        val request = MockHttpServletRequest("GET", "/saml2/authenticate/$registrationId").apply {
            scheme = "http"
            serverName = "internal-task"
            serverPort = 8080
            addHeader("X-Forwarded-Proto", "https")
            addHeader("X-Forwarded-Host", "elsapalvelu.fi")
            addHeader("X-Forwarded-Port", "443")
        }
        ForwardedHeaderFilter().doFilter(request, MockHttpServletResponse()) { forwarded, _ ->
            val externalRequest = forwarded as HttpServletRequest
            RequestContextHolder.setRequestAttributes(ServletRequestAttributes(externalRequest))
            val authnRequest = requireNotNull(resolver.resolve<AbstractSaml2AuthenticationRequest>(externalRequest))
            val xml = xml(authnRequest.samlRequest)
            assertThat(xml.documentElement.getAttribute("AssertionConsumerServiceURL"))
                .isEqualTo("https://elsapalvelu.fi/login/saml2/sso/$registrationId")
            assertThat(xml.getElementsByTagNameNS(Issuer.DEFAULT_ELEMENT_NAME.namespaceURI, "Issuer").item(0).textContent)
                .isEqualTo("https://elsapalvelu.fi/saml/$issuerSuffix")
            assertThat(authnRequest.relayState).isEqualTo("elsa")
        }
    }

    @ParameterizedTest
    @CsvSource("haka-tampere,haka,true", "haka-helsinki,haka,true", "haka-tampere,suomifi,false", "suomifi,suomifi,true", "suomifi,haka,false")
    fun assertionAudienceIsRestrictedToItsRegistration(registrationId: String, audienceSuffix: String, valid: Boolean) {
        val validator = testSecurityConfiguration().createAssertionValidator()
        assertThat(requireNotNull(validator.convert(assertionToken(registrationId, audienceSuffix))).hasErrors()).isEqualTo(!valid)
    }

    @Test
    fun assertionValidatorRetainsClockSkewAndRejectsExpiredAssertions() {
        val validator = testSecurityConfiguration().createAssertionValidator()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-tampere", "haka", 120))).hasErrors()).isFalse()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-tampere", "haka", 600))).hasErrors()).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["issuer", "recipient"])
    fun customAudienceValidationStillRejectsWrongIssuerOrRecipient(field: String) {
        val validator = testSecurityConfiguration().createAssertionValidator()
        val token = assertionToken("haka-tampere", "haka")
        if (field == "issuer") {
            requireNotNull(token.assertion.issuer).value = "https://other-idp.example"
        } else {
            val subject = requireNotNull(token.assertion.subject)
            requireNotNull(subject.subjectConfirmations.single().subjectConfirmationData).recipient = "https://other-sp.example/acs"
        }
        assertThat(requireNotNull(validator.convert(token)).hasErrors()).isTrue()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-tampere", "haka"))).hasErrors()).isFalse()
    }

    @Test
    fun oneValidatorHandlesMultipleRegistrationAudiencesWithoutLeakingConfiguration() {
        val validator = testSecurityConfiguration().createAssertionValidator()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-tampere", "haka"))).hasErrors()).isFalse()
        assertThat(requireNotNull(validator.convert(assertionToken("suomifi", "suomifi"))).hasErrors()).isFalse()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-tampere", "suomifi"))).hasErrors()).isTrue()
        assertThat(requireNotNull(validator.convert(assertionToken("haka-helsinki", "haka"))).hasErrors()).isFalse()
    }

    @ParameterizedTest
    @CsvSource("haka-tampere", "suomifi")
    fun logoutPreservesExternalNameIdQualifiersAndSessionIndexes(registrationId: String) {
        val registration = registration(registrationId)
        val source = testSamlPrincipal("external-subject", mapOf(
            "nameID" to listOf("external-subject"),
            "nameIDFormat" to listOf(NameIDType.PERSISTENT),
            "nameIDQualifier" to listOf("https://idp.example"),
            "nameIDSPQualifier" to listOf("https://elsapalvelu.fi")
        ), listOf("session-one", "session-two"))
        val config = testSecurityConfiguration()
        val principal = config.createPrincipal("internal-user-id", source, registrationId)
        val resolver = config.logoutRequestResolver(DefaultRelyingPartyRegistrationResolver(
            InMemoryRelyingPartyRegistrationRepository(registration)
        ))
        val request = MockHttpServletRequest().apply {
            scheme = "https"
            serverName = "elsapalvelu.fi"
            serverPort = 443
        }
        val logout = requireNotNull(resolver.resolve(request, Saml2Authentication(principal, "response", emptyList())))
        val xml = xml(logout.samlRequest)
        val nameId = xml.getElementsByTagNameNS(NameID.DEFAULT_ELEMENT_NAME.namespaceURI, "NameID").item(0)
        assertThat(nameId.textContent).isEqualTo("external-subject")
        assertThat(nameId.attributes.getNamedItem("Format").nodeValue).isEqualTo(NameIDType.PERSISTENT)
        assertThat(nameId.attributes.getNamedItem("NameQualifier").nodeValue).isEqualTo("https://idp.example")
        assertThat(nameId.attributes.getNamedItem("SPNameQualifier").nodeValue).isEqualTo("https://elsapalvelu.fi")
        val indexes = xml.getElementsByTagNameNS(SessionIndex.DEFAULT_ELEMENT_NAME.namespaceURI, "SessionIndex")
        assertThat((0 until indexes.length).map { indexes.item(it).textContent }).containsExactly("session-one", "session-two")
    }

    private fun registration(id: String): RelyingPartyRegistration {
        val certificate = HeldCertificate.Builder().rsa2048().commonName("saml-test").build()
        return RelyingPartyRegistration.withRegistrationId(id)
            .entityId("{baseUrl}/saml/$id")
            .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
            .signingX509Credentials { it.add(Saml2X509Credential.signing(certificate.keyPair.private, certificate.certificate)) }
            .assertingPartyMetadata {
                it.entityId("https://idp.example")
                    .singleSignOnServiceLocation("https://idp.example/login")
                    .singleSignOnServiceBinding(Saml2MessageBinding.POST)
                    .singleLogoutServiceLocation("https://idp.example/logout")
                    .singleLogoutServiceBinding(Saml2MessageBinding.POST)
                    .wantAuthnRequestsSigned(true)
            }.build()
    }

    private fun assertionToken(id: String, audienceSuffix: String, expiredSeconds: Long = -60): OpenSaml5AuthenticationProvider.AssertionToken {
        val registration = registration(id).mutate().entityId("https://elsapalvelu.fi/saml/$id")
            .assertionConsumerServiceLocation("https://elsapalvelu.fi/login/saml2/sso/$id").build()
        val assertion = saml<Assertion>(Assertion.DEFAULT_ELEMENT_NAME).apply {
            this.id = "assertion-test"
            issueInstant = Instant.now()
            issuer = saml<Issuer>(Issuer.DEFAULT_ELEMENT_NAME).apply { value = "https://idp.example" }
            conditions = saml<Conditions>(Conditions.DEFAULT_ELEMENT_NAME).apply {
                notBefore = Instant.now().minusSeconds(1200)
                notOnOrAfter = Instant.now().minusSeconds(expiredSeconds)
                audienceRestrictions.add(saml<AudienceRestriction>(AudienceRestriction.DEFAULT_ELEMENT_NAME).apply {
                    audiences.add(saml<Audience>(Audience.DEFAULT_ELEMENT_NAME).apply { uri = "https://elsapalvelu.fi/saml/$audienceSuffix" })
                })
            }
            subject = saml<Subject>(Subject.DEFAULT_ELEMENT_NAME).apply {
                subjectConfirmations.add(saml<SubjectConfirmation>(SubjectConfirmation.DEFAULT_ELEMENT_NAME).apply {
                    method = SubjectConfirmation.METHOD_BEARER
                    subjectConfirmationData = saml<SubjectConfirmationData>(SubjectConfirmationData.DEFAULT_ELEMENT_NAME).apply {
                        recipient = registration.assertionConsumerServiceLocation
                        notOnOrAfter = Instant.now().plusSeconds(600)
                    }
                })
            }
        }
        saml<Response>(Response.DEFAULT_ELEMENT_NAME).assertions.add(assertion)
        // AssertionToken is package-private; this fixture tests assertion validation, not signature verification.
        val constructor = OpenSaml5AuthenticationProvider.AssertionToken::class.java
            .getDeclaredConstructor(Assertion::class.java, Saml2AuthenticationToken::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(assertion, Saml2AuthenticationToken(registration, "response"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : XMLObject> saml(element: QName): T =
        XMLObjectProviderRegistrySupport.getBuilderFactory().ensureBuilder<XMLObject>(element).buildObject(element) as T

    private fun xml(encoded: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(Base64.getDecoder().decode(encoded).inputStream())
}
