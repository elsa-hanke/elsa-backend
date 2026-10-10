package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.ApplicationProperties
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ElsaUriFilterTest {
    private val properties = ApplicationProperties().apply { getSecurity().samlScheme = "https" }
    private val filter = ElsaUriFilter(properties)

    @Test
    fun alreadySecureRequestKeepsItsSchemeAndPort() {
        val request = MockHttpServletRequest().apply {
            scheme = "https"
            serverPort = 8443
        }
        filter.doFilter(request, MockHttpServletResponse()) { forwarded, _ ->
            val observed = forwarded as HttpServletRequest
            assertThat(observed.scheme).isEqualTo("https")
            assertThat(observed.serverPort).isEqualTo(8443)
        }
    }

    @Test
    fun internalHttpRequestUsesConfiguredExternalSchemeAndPort() {
        val request = MockHttpServletRequest().apply {
            scheme = "http"
            serverPort = 8080
        }
        filter.doFilter(request, MockHttpServletResponse()) { forwarded, _ ->
            val observed = forwarded as HttpServletRequest
            assertThat(observed.scheme).isEqualTo("https")
            assertThat(observed.serverPort).isEqualTo(443)
        }
    }
}
