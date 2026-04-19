package fi.elsapalvelu.elsa.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.web.filter.OncePerRequestFilter

private const val MDC_USER_ID_KEY = "userId"

/**
 * Adds the authenticated user's ID to SLF4J MDC for every request so that
 * all log entries within a request can be correlated to a specific user.
 *
 * The key "userId" is available in Logback patterns as %X{userId}.
 */
class MdcUserIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = try {
            val principal = SecurityContextHolder.getContext().authentication?.principal
            if (principal is Saml2AuthenticatedPrincipal) principal.name else null
        } catch (_: Exception) {
            null
        }

        try {
            if (userId != null) {
                MDC.put(MDC_USER_ID_KEY, userId)
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_USER_ID_KEY)
        }
    }
}

