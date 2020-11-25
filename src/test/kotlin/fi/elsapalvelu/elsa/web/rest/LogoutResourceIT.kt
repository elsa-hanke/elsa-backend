package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class LogoutResourceIT {

    @Autowired
    private lateinit var registrations: ClientRegistrationRepository

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var restLogoutMockMvc: MockMvc

    private lateinit var idToken: OidcIdToken

    @BeforeEach
    @Throws(Exception::class)
    fun before() {
        val claims = mapOf(
            "groups" to listOf("ROLE_USER"),
            "sub" to 123
        )
        val idToken = OidcIdToken(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims)
        SecurityContextHolder.getContext().authentication = authenticationToken(idToken)
        val authInjector = SecurityContextHolderAwareRequestFilter()
        authInjector.afterPropertiesSet()

        restLogoutMockMvc = MockMvcBuilders.webAppContextSetup(this.context).build()
    }

    @Test
    @Throws(Exception::class)
    fun getLogoutInformation() {
        val logoutUrl = this.registrations.findByRegistrationId("oidc").providerDetails
            .configurationMetadata["end_session_endpoint"].toString()
        restLogoutMockMvc.perform(post("/api/logout"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("\$.logoutUrl").value(logoutUrl))
            .andExpect(jsonPath("\$.idToken").value(ID_TOKEN))
    }
}
