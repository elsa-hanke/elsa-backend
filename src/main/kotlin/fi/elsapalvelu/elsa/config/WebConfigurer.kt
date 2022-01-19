package fi.elsapalvelu.elsa.config

import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import tech.jhipster.config.JHipsterProperties
import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
class WebConfigurer(
    private val env: Environment,
    private val jHipsterProperties: JHipsterProperties
) : ServletContextInitializer {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(ServletException::class)
    override fun onStartup(servletContext: ServletContext) {
        if (env.activeProfiles.isNotEmpty()) {
            log.info(
                "Web application configuration, using profiles: {}",
                *env.activeProfiles as Array<*>
            )
        }

        servletContext.sessionCookieConfig.isSecure = true

        log.info("Web application fully configured")
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = jHipsterProperties.cors
        if (config.allowedOriginPatterns != null && config.allowedOriginPatterns!!.isNotEmpty()) {
            log.debug("Registering CORS filter")
            source.apply {
                registerCorsConfiguration("/api/**", config)
                registerCorsConfiguration("/management/**", config)
            }
        }
        return CorsFilter(source)
    }
}
