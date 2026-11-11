package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.security.LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE
import fi.elsapalvelu.elsa.security.LoginNameMismatch
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.assertj.core.api.Assertions.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RedirectResourceTest {

    private val now = Instant.parse("2026-10-16T10:00:00Z")
    private val mvc: MockMvc = mvcAt(now)
    private val endpoint = "/api/julkinen/kirjautuminen/nimivirhe"

    @Test
    fun `name mismatch details belong only to the failed login session and are not cached`() {
        val session = failedLoginSession(LoginException.VIRHEELLINEN_NIMI)

        mvc.perform(get(endpoint).session(session))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.invitationName").value("Aleksi Räisänen"))
            .andExpect(jsonPath("$.authenticatedName").value("Kari Räisänen"))
            .andExpect(jsonPath("$.expiresAt").doesNotExist())

        mvc.perform(get(endpoint))
            .andExpect(status().isNoContent)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(""))
    }

    @Test
    fun `other login errors do not expose stale name details`() {
        mvc.perform(get(endpoint).session(failedLoginSession(LoginException.EI_KAYTTO_OIKEUTTA)))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
    }

    @Test
    fun `name error without details still permits the existing login page`() {
        val session = failedLoginSession(LoginException.VIRHEELLINEN_NIMI)
        session.removeAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE)

        mvc.perform(get(endpoint).session(session))
            .andExpect(status().isNoContent)
        mvc.perform(get("/kirjaudu").session(session))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("http://localhost/kirjautuminen?virhe=VIRHEELLINEN_NIMI"))
    }

    @Test
    fun `details are available immediately before expiry but removed at the five minute deadline`() {
        val session = failedLoginSession(LoginException.VIRHEELLINEN_NIMI)
        mvcAt(now.plusSeconds(300).minusNanos(1))
            .perform(get(endpoint).session(session))
            .andExpect(status().isOk)

        mvcAt(now.plusSeconds(300)).perform(get(endpoint).session(session))
            .andExpect(status().isNoContent)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(""))
        assertThat(session.getAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE)).isNull()
    }

    private fun mvcAt(instant: Instant): MockMvc = MockMvcBuilders.standaloneSetup(
        RedirectResource(MockEnvironment(), Clock.fixed(instant, ZoneOffset.UTC))
    ).build()

    private fun failedLoginSession(error: LoginException) = MockHttpSession().apply {
        setAttribute(
            WebAttributes.AUTHENTICATION_EXCEPTION,
            Saml2AuthenticationException(Saml2Error("invalid_response", error.name), error.name)
        )
        setAttribute(
            LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE,
            LoginNameMismatch("Aleksi Räisänen", "Kari Räisänen", now.plusSeconds(300))
        )
    }
}
