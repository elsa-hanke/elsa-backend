package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.required

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
                val originalScheme = super.getScheme()
                return if (originalScheme == "http") {
                    applicationProperties.getSecurity().samlScheme.required()
                } else {
                    originalScheme
                }
            }

            override fun getServerPort(): Int {
                return if (super.getScheme() == "http" && applicationProperties.getSecurity().samlScheme == "https") {
                    443
                } else {
                    super.getServerPort()
                }
            }
        }, response)
    }
}
