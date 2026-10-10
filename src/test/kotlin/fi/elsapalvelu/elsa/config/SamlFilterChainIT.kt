package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.ElsaBackendApp
import jakarta.servlet.Filter
import okhttp3.tls.HeldCertificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.ForwardedHeaderFilter
import java.util.Base64

@ActiveProfiles("dev", "test")
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "application.security.saml-scheme=https",
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///elsaBackend?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "spring.liquibase.change-log=classpath:/config/liquibase/test-master.xml",
    "spring.liquibase.contexts=test"
])
class SamlFilterChainIT {
    @Autowired private lateinit var context: WebApplicationContext
    @Autowired @Qualifier("springSecurityFilterChain") private lateinit var securityFilterChain: Filter
    @MockitoBean private lateinit var registrations: RelyingPartyRegistrationRepository

    private val mockMvc: MockMvc by lazy {
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(ForwardedHeaderFilter(), securityFilterChain)
            .build()
    }

    @BeforeEach
    fun registerSamlParties() {
        val certificate = HeldCertificate.Builder().rsa2048().commonName("saml-test").build()
        listOf("suomifi", "haka-tampere", "haka-helsinki").forEach { id ->
            val registration = RelyingPartyRegistration.withRegistrationId(id)
                .entityId("{baseUrl}/saml/$id")
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .signingX509Credentials { it.add(Saml2X509Credential.signing(certificate.keyPair.private, certificate.certificate)) }
                .assertingPartyMetadata {
                    it.entityId("https://idp.example")
                        .singleSignOnServiceLocation("https://idp.example/login")
                        .singleSignOnServiceBinding(Saml2MessageBinding.POST)
                }.build()
            `when`(registrations.findByRegistrationId(id)).thenReturn(registration)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["suomifi", "haka-tampere", "haka-helsinki"])
    fun forwardedHttpsSamlRequestUsesValidSchemeWithSession(registrationId: String) {
        val session = MockHttpSession()
        mockMvc.perform(get("/api/ping").session(session)).andExpect(status().isOk)

        val result = mockMvc.perform(get("/saml2/authenticate/$registrationId")
            .session(session)
            .header("X-Forwarded-Proto", "https")
            .header("X-Forwarded-Host", "elsapalvelu.fi")
            .header("X-Forwarded-Port", "443"))
            .andExpect(status().isOk)
            .andReturn()

        assertSamlUrl(result, "https://elsapalvelu.fi/login/saml2/sso/$registrationId")

        mockMvc.perform(get("/api/ping").session(session)
            .header("X-Forwarded-Proto", "https")
            .header("X-Forwarded-Host", "elsapalvelu.fi")
            .header("X-Forwarded-Port", "443"))
            .andExpect(status().isOk)
        assertThat(session.isInvalid).isFalse()
    }

    @Test
    fun internalHttpRequestUsesConfiguredHttpsInSamlUrl() {
        val result = mockMvc.perform(get("/saml2/authenticate/haka-tampere"))
            .andExpect(status().isOk)
            .andReturn()
        assertSamlUrl(result, "https://localhost/login/saml2/sso/haka-tampere")
    }

    @Test
    fun alreadyHttpsRequestKeepsItsNonstandardPortInSamlUrl() {
        val result = mockMvc.perform(get("/saml2/authenticate/haka-tampere").with {
            it.scheme = "https"
            it.serverName = "elsapalvelu.fi"
            it.serverPort = 8443
            it.isSecure = true
            it
        }).andExpect(status().isOk).andReturn()
        assertSamlUrl(result, "https://elsapalvelu.fi:8443/login/saml2/sso/haka-tampere")
    }

    private fun assertSamlUrl(result: MvcResult, expectedUrl: String) {
        val encoded = Regex("name=\"SAMLRequest\" type=\"hidden\" value=\"([^\"]+)\"")
            .find(result.response.contentAsString)?.groupValues?.get(1)
        val xml = String(Base64.getDecoder().decode(requireNotNull(encoded)))
        assertThat(xml).contains(expectedUrl)
        assertThat(xml).doesNotContain("httpss")
    }
}
