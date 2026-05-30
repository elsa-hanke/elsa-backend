package fi.elsapalvelu.elsa.config.management

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Permits all requests arriving on the dedicated management port (8081).
 * Network-level access is restricted by the EC2 SSM security group → ECS task
 * security group rule, so no application-level auth is needed here.
 *
 * Must be @Order(0) to take priority over the main SecurityConfiguration filter chain.
 */
@Configuration
class ManagementSecurityConfiguration {

    @Bean
    @Order(0)
    fun managementPortSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher { request -> request.localPort == MANAGEMENT_PORT }
        http.authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
        http.csrf { csrf -> csrf.disable() }
        return http.build()
    }

    companion object {
        const val MANAGEMENT_PORT = 8081
    }
}

