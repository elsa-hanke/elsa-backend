package fi.elsapalvelu.elsa.security

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class StaleXsrfCookieClearingFilterTest {

    private fun filter(domain: String = "kehitys.elsapalvelu.fi") =
        StaleXsrfCookieClearingFilter(domain)

    private fun run(
        filter: StaleXsrfCookieClearingFilter,
        vararg cookies: Cookie
    ): MockHttpServletResponse {
        val request = MockHttpServletRequest().apply { setCookies(*cookies) }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response
    }

    private fun xsrf(value: String) = Cookie("XSRF-TOKEN", value)

    @Test
    fun `does nothing when no cookies at all`() {
        val response = run(filter())
        assertThat(response.cookies).isEmpty()
    }

    @Test
    fun `does nothing when only one XSRF-TOKEN cookie present`() {
        val response = run(filter(), xsrf("only-token"))
        assertThat(response.cookies).isEmpty()
    }

    @Test
    fun `expires parent domain cookie when two XSRF-TOKEN cookies present`() {
        val response = run(filter(), xsrf("stale-parent-token"), xsrf("current-subdomain-token"))

        val expiringCookies = response.cookies.filter { it.name == "XSRF-TOKEN" }
        assertThat(expiringCookies).hasSize(1)

        val expiring = expiringCookies.single()
        assertThat(expiring.domain).isEqualTo("elsapalvelu.fi")
        assertThat(expiring.maxAge).isEqualTo(0)
        assertThat(expiring.value).isEmpty()
        assertThat(expiring.path).isEqualTo("/")
        assertThat(expiring.secure).isTrue()
    }

    @Test
    fun `expires all intermediate parent domains for a deeply nested subdomain`() {
        // e.g. sub.kehitys.elsapalvelu.fi -> parents: kehitys.elsapalvelu.fi, elsapalvelu.fi
        val deepFilter = filter("sub.kehitys.elsapalvelu.fi")
        val response = run(deepFilter, xsrf("token-a"), xsrf("token-b"))

        val expiredDomains = response.cookies.filter { it.name == "XSRF-TOKEN" }.map { it.domain }
        assertThat(expiredDomains).containsExactlyInAnyOrder("kehitys.elsapalvelu.fi", "elsapalvelu.fi")
    }

    @Test
    fun `does not expire anything when csrfCookieDomain is a top-level two-part domain (prod)`() {
        // 'elsapalvelu.fi' has no parent to clear — avoids wiping prod unnecessarily
        val prodFilter = filter("elsapalvelu.fi")
        val response = run(prodFilter, xsrf("token-a"), xsrf("token-b"))
        assertThat(response.cookies).isEmpty()
    }

    @Test
    fun `does not add expiry cookies when csrfCookieDomain is blank`() {
        val response = run(filter(""), xsrf("token-a"), xsrf("token-b"))
        assertThat(response.cookies).isEmpty()
    }

    // -------------------------------------------------------------------------
    // Non-XSRF cookies must not be touched
    // -------------------------------------------------------------------------

    @Test
    fun `ignores non-XSRF cookies when counting duplicates`() {
        // Only one XSRF-TOKEN plus unrelated cookies — should do nothing
        val response = run(
            filter(),
            Cookie("JSESSIONID", "abc"),
            xsrf("only-xsrf"),
            Cookie("AWSALB", "xyz")
        )
        assertThat(response.cookies).isEmpty()
    }

    @Test
    fun `only expires XSRF-TOKEN, never other cookie names`() {
        val response = run(filter(), xsrf("stale"), xsrf("current"))
        assertThat(response.cookies.map { it.name }).containsOnly("XSRF-TOKEN")
    }

    // -------------------------------------------------------------------------
    // Filter chain must always continue
    // -------------------------------------------------------------------------

    @Test
    fun `filter chain is always invoked regardless of cookie state`() {
        val chain = MockFilterChain()
        val request = MockHttpServletRequest().apply { setCookies(xsrf("only-token")) }
        filter().doFilter(request, MockHttpServletResponse(), chain)
        // MockFilterChain records the request it was called with; null means it was never called
        assertThat(chain.request).isNotNull()
    }

    @Test
    fun `filter chain is invoked even when stale cookie is cleared`() {
        val chain = MockFilterChain()
        val request = MockHttpServletRequest().apply { setCookies(xsrf("stale"), xsrf("current")) }
        filter().doFilter(request, MockHttpServletResponse(), chain)
        assertThat(chain.request).isNotNull()
    }
}

