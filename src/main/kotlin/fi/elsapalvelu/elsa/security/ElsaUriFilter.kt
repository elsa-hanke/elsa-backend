package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse

/**
 * AWS:n load balancer muuntaa https yhteydet http:ksi, jolloin SAML pyyntöihin muodostuu
 * väärä scheme
 */
class ElsaUriFilter(private val applicationProperties: ApplicationProperties) :
    OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        filterChain.doFilter(object : HttpServletRequestWrapper(request) {
            override fun getScheme(): String {
                return super.getScheme()
                    .replace("http", applicationProperties.getSecurity().samlScheme!!)
            }

            override fun getServerPort(): Int {
                return if (applicationProperties.getSecurity().samlScheme == "http") super.getServerPort() else 443
            }
        }, response)
    }
}
