package fi.elsapalvelu.elsa.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import liquibase.integration.spring.SpringLiquibase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ExternalIntegrationProfileTest {
    @Test
    fun externalIntegrationProfileStartsWithoutDatabaseInfrastructure() {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=external-integration")
            .withUserConfiguration(ExternalClientConfiguration::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.environment.activeProfiles).contains("external-integration")
                // Load the real test configuration: its default DB requires Docker,
                // but the external profile must prevent any database initialization.
                assertThat(context.environment.getProperty("spring.datasource.url"))
                    .startsWith("jdbc:tc:postgresql:")
                assertThat(context).doesNotHaveBean(DataSource::class.java)
                assertThat(context).doesNotHaveBean(EntityManagerFactory::class.java)
                assertThat(context).doesNotHaveBean(SpringLiquibase::class.java)
                assertThat(context).hasSingleBean(ObjectMapper::class.java)
            }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class ExternalClientConfiguration
}
