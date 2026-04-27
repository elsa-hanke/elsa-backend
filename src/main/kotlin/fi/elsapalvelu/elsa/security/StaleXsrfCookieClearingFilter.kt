package fi.elsapalvelu.elsa.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Clears stale XSRF-TOKEN cookies that are scoped to a parent domain when the
 * application is configured to use a more specific subdomain for its CSRF cookie.
 *
 * Background: the CSRF cookie domain was previously set to 'elsapalvelu.fi' for all
 * environments. After it was changed to per-subdomain (e.g. 'kehitys.elsapalvelu.fi'),
 * browsers that visited the app before the change still carry the old parent-domain
 * XSRF-TOKEN cookie alongside the new subdomain one. Spring's WebUtils.getCookie()
 * returns the first matching cookie, which is the stale parent-domain one, causing
 * CSRF validation to fail with a 403.
 *
 * This filter detects when a request carries an XSRF-TOKEN cookie on a different
 * (parent) domain than the configured csrf cookie domain and expires it, so that
 * after one request the browser drops the stale cookie permanently.
 */
class StaleXsrfCookieClearingFilter(private val csrfCookieDomain: String) :
    OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        clearStaleCookiesIfNeeded(request, response)
        filterChain.doFilter(request, response)
    }

    private fun clearStaleCookiesIfNeeded(request: HttpServletRequest, response: HttpServletResponse) {
        val cookies = request.cookies ?: return

        // Count how many XSRF-TOKEN cookies are in the request.
        // More than one means there is at least one stale cookie on a different domain.
        val xsrfCookies = cookies.filter { it.name == XSRF_TOKEN_COOKIE_NAME }
        if (xsrfCookies.size <= 1) return

        // Derive the parent domain from the configured csrf cookie domain.
        // e.g. 'kehitys.elsapalvelu.fi' -> parent candidates are 'elsapalvelu.fi'
        val parentDomains = parentDomainsOf(csrfCookieDomain)

        parentDomains.forEach { parentDomain ->
            val expiring = Cookie(XSRF_TOKEN_COOKIE_NAME, "")
            expiring.domain = parentDomain
            expiring.path = "/"
            expiring.maxAge = 0
            expiring.secure = true
            response.addCookie(expiring)
        }
    }

    /**
     * Returns all parent domain suffixes for a given hostname.
     * e.g. 'kehitys.elsapalvelu.fi' -> ['elsapalvelu.fi']
     */
    private fun parentDomainsOf(domain: String): List<String> {
        val parts = domain.split(".")
        // Need at least 3 parts (sub.example.com) to have a meaningful parent
        if (parts.size <= 2) return emptyList()
        return (1 until parts.size - 1).map { i -> parts.drop(i).joinToString(".") }
    }

    companion object {
        private const val XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN"
    }
}

