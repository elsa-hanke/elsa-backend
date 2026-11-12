package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import okhttp3.tls.HeldCertificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ResourceLoader
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutRequestValidatorParametersResolver
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.writeText

@ExtendWith(MockitoExtension::class)
class RelyingPartyConfigurationTest {

    @Mock
    private lateinit var resourceLoader: ResourceLoader

    @Mock
    private lateinit var yliopistoRepository: YliopistoRepository

    @TempDir
    lateinit var temporaryDirectory: Path

    @ParameterizedTest
    @ValueSource(strings = ["suomifi", "haka-tampere"])
    fun `incoming logout without authentication resolves SP location`(registrationId: String) {
        val certificate = HeldCertificate.Builder().rsa2048().commonName("saml-test").build()
        whenever(resourceLoader.getResource("certificate"))
            .thenReturn(ByteArrayResource(certificate.certificatePem().toByteArray()))
        whenever(resourceLoader.getResource("private-key"))
            .thenReturn(ByteArrayResource(certificate.privateKeyPkcs8Pem().toByteArray()))
        val metadata = metadataLocation(certificate)
        val properties = ApplicationProperties().apply {
            if (registrationId == "suomifi") {
                getSecurity().getSuomifi().apply {
                    enabled = true
                    samlCertificateLocation = "certificate"
                    samlPrivateKeyLocation = "private-key"
                    samlSuomifiMetadataLocation = metadata
                    samlSuomifiEntityId = "https://idp.example"
                }
            } else {
                getSecurity().getHaka().apply {
                    enabled = true
                    samlCertificateLocation = "certificate"
                    samlPrivateKeyLocation = "private-key"
                    samlHakaMetadataLocation = metadata
                }
            }
        }
        if (registrationId != "suomifi") {
            whenever(yliopistoRepository.findAllHaka())
                .thenReturn(mutableListOf(Yliopisto(hakaId = registrationId, hakaEntityId = "https://idp.example")))
        }
        val registrations = requireNotNull(configuration(properties).relyingPartyRegistrations())
        val resolver = OpenSaml5LogoutRequestValidatorParametersResolver(registrations)
        val request = MockHttpServletRequest("GET", "/logout/saml2/slo").apply {
            scheme = "https"
            serverName = "elsapalvelu.fi"
            serverPort = 443
            addParameter("SAMLRequest", encodedLogoutRequest())
        }

        val parameters = requireNotNull(resolver.resolve(request, null))

        assertThat(parameters.relyingPartyRegistration.registrationId).isEqualTo(registrationId)
        assertThat(parameters.logoutRequest.location).isEqualTo("https://elsapalvelu.fi/logout/saml2/slo")
        assertThat(parameters.logoutRequest.binding).isEqualTo(Saml2MessageBinding.REDIRECT)
        assertThat(parameters.relyingPartyRegistration.singleLogoutServiceResponseLocation)
            .isEqualTo("https://elsapalvelu.fi/logout/saml2/slo")
    }

    @Test
    fun `disabled identity providers do not require SAML configuration`() {
        val registrations = configuration(ApplicationProperties()).relyingPartyRegistrations()

        assertThat(registrations).isNull()
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    @Test
    fun `enabled Suomi identity provider fails fast when certificate location is missing`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getSuomifi().enabled = true
        }

        assertThrows<IllegalArgumentException> {
            configuration(properties).relyingPartyRegistrations()
        }
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    @Test
    fun `enabled Haka identity provider fails fast when certificate location is missing`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getHaka().enabled = true
        }

        assertThrows<IllegalArgumentException> {
            configuration(properties).relyingPartyRegistrations()
        }
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    private fun configuration(properties: ApplicationProperties) = RelyingPartyConfiguration(
        resourceLoader,
        properties,
        yliopistoRepository
    )

    private fun metadataLocation(certificate: HeldCertificate): String = temporaryDirectory.resolve("metadata.xml").apply {
        writeText("""
            <EntitiesDescriptor xmlns="urn:oasis:names:tc:SAML:2.0:metadata">
              <EntityDescriptor entityID="https://idp.example">
                <IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
                  <KeyDescriptor use="signing">
                    <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#"><X509Data><X509Certificate>
                      ${Base64.getEncoder().encodeToString(certificate.certificate.encoded)}
                    </X509Certificate></X509Data></KeyInfo>
                  </KeyDescriptor>
                  <SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
                                      Location="https://idp.example/login"/>
                </IDPSSODescriptor>
              </EntityDescriptor>
            </EntitiesDescriptor>
        """.trimIndent())
    }.toUri().toString()

    private fun encodedLogoutRequest(): String {
        val xml = """
            <saml2p:LogoutRequest xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                                 ID="logout-test" Version="2.0" IssueInstant="2026-11-11T10:00:00Z">
              <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">https://idp.example</saml2:Issuer>
            </saml2p:LogoutRequest>
        """.trimIndent()
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        val output = ByteArrayOutputStream()
        try {
            DeflaterOutputStream(output, deflater).use { it.write(xml.toByteArray()) }
        } finally {
            deflater.end()
        }
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }
}
