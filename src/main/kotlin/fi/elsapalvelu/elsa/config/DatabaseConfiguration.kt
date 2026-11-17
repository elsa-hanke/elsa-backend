package fi.elsapalvelu.elsa.config

import org.springframework.context.annotation.Bean
import java.time.Clock
import java.time.Instant
import java.util.Optional
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories("fi.elsapalvelu.elsa.repository")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware", dateTimeProviderRef = "auditoinninAika")
@EnableTransactionManagement
class DatabaseConfiguration(private val clock: Clock) {
    @Bean
    fun auditoinninAika(): DateTimeProvider = DateTimeProvider { Optional.of(Instant.now(clock)) }
}
