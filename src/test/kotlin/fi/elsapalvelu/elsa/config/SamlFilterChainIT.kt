package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.security.LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE
import fi.elsapalvelu.elsa.security.LoginNameMismatch
import jakarta.servlet.Filter
import okhttp3.tls.HeldCertificate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.ForwardedHeaderFilter
import java.util.Base64
import java.time.Instant

@ActiveProfiles("dev", "test")
@SpringBootTest(classes = [ElsaBackendApp::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "application.security.saml-scheme=https",
    "server.forward-headers-strategy=native",
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///elsaBackend?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "spring.liquibase.change-log=classpath:/config/liquibase/test-master.xml",
    "spring.liquibase.contexts=test"
])
class SamlFilterChainIT {
    @Autowired private lateinit var context: WebApplicationContext
    @Autowired private lateinit var serverContext: ServletWebServerApplicationContext
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
    fun albHeadersUseApiHostForSuomifiSamlRequestThroughTomcat() {
        val port = requireNotNull(serverContext.webServer).port
        val request = Request.Builder()
            .url("http://localhost:$port/saml2/authenticate/suomifi")
            .header("Host", "api.elsapalvelu.fi")
            .header("X-Forwarded-Proto", "https")
            .header("X-Forwarded-Port", "443")
            .build()

        OkHttpClient.Builder().followRedirects(false).build().newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
            val xml = assertSamlUrl(requireNotNull(response.body).string(),
                "https://api.elsapalvelu.fi/login/saml2/sso/suomifi")
            assertThat(xml).contains("https://api.elsapalvelu.fi/saml/suomifi")
        }
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

    @Test
    fun nameMismatchDetailsStayInTheirOwnFailedLoginSessionThroughSecurityFilters() {
        val endpoint = "/api/julkinen/kirjautuminen/nimivirhe"
        val firstSession = failedNameLogin(LoginNameMismatch("Aleksi Räisänen", "Kari Räisänen"))
        val secondSession = failedNameLogin(LoginNameMismatch("Mia Haavisto", "Anna Mia Haavisto"))

        mockMvc.perform(get("/kirjaudu").session(firstSession))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("http://localhost:9060/kirjautuminen?virhe=VIRHEELLINEN_NIMI"))
        mockMvc.perform(get(endpoint).session(firstSession))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.invitationName").value("Aleksi Räisänen"))
            .andExpect(jsonPath("$.authenticatedName").value("Kari Räisänen"))
            .andExpect(jsonPath("$.expiresAt").doesNotExist())

        mockMvc.perform(get(endpoint).session(secondSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.invitationName").value("Mia Haavisto"))
            .andExpect(jsonPath("$.authenticatedName").value("Anna Mia Haavisto"))

        mockMvc.perform(get(endpoint).session(MockHttpSession())
            .param("userId", "invited-user")
            .param("virhe", LoginException.VIRHEELLINEN_NIMI.name))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
        mockMvc.perform(get(endpoint))
            .andExpect(status().isNoContent)
            .andExpect(header().string("Cache-Control", "no-store"))

        // Reading login error details must not authenticate either session.
        listOf(firstSession, secondSession).forEach { session ->
            mockMvc.perform(get("/api/kayttaja").session(session))
                .andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun expiredNameMismatchDetailsAreRemovedThroughSecurityFilters() {
        val session = failedNameLogin(LoginNameMismatch("Aleksi Räisänen", "Kari Räisänen", Instant.EPOCH))

        mockMvc.perform(get("/api/julkinen/kirjautuminen/nimivirhe").session(session))
            .andExpect(status().isNoContent)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(""))

        assertThat(session.getAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE)).isNull()
        mockMvc.perform(get("/kirjaudu").session(session))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("http://localhost:9060/kirjautuminen?virhe=VIRHEELLINEN_NIMI"))
    }

    private fun failedNameLogin(mismatch: LoginNameMismatch) = MockHttpSession().apply {
        setAttribute(
            WebAttributes.AUTHENTICATION_EXCEPTION,
            Saml2AuthenticationException(
                Saml2Error("invalid_response", LoginException.VIRHEELLINEN_NIMI.name),
                LoginException.VIRHEELLINEN_NIMI.name
            )
        )
        setAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE, mismatch)
    }

    private fun assertSamlUrl(result: MvcResult, expectedUrl: String) {
        assertSamlUrl(result.response.contentAsString, expectedUrl)
    }

    private fun assertSamlUrl(html: String, expectedUrl: String): String {
        val encoded = Regex("name=\"SAMLRequest\" type=\"hidden\" value=\"([^\"]+)\"")
            .find(html)?.groupValues?.get(1)
        val xml = String(Base64.getDecoder().decode(requireNotNull(encoded)))
        assertThat(xml).contains(expectedUrl)
        assertThat(xml).doesNotContain("httpss")
        return xml
    }
}
