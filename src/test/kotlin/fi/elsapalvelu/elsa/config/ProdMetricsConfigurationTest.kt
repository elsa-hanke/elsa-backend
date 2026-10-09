package fi.elsapalvelu.elsa.config

import io.micrometer.core.instrument.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.env.MockPropertySource
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

class ProdMetricsConfigurationTest {
    @Test
    fun prodMetricAllowlistIncludesSessionsAndRejectsUnexpectedMetrics() {
        val environment = StandardEnvironment().apply {
            propertySources.addFirst(YamlPropertySourceLoader().load("prod", ClassPathResource("config/application-prod.yml")).single())
            // Exercise registry/filter behavior without publishing to AWS.
            propertySources.addFirst(MockPropertySource().withProperty("management.cloudwatch2.metrics.export.enabled", "false"))
        }
        val configuration = CloudWatchMeterRegistryConfiguration(environment)
        val config = configuration.cloudWatchConfig()
        assertThat(config.namespace()).isEqualTo("elsa")
        val registry = configuration.cloudWatchMeterRegistry(config, Clock.SYSTEM, mock(CloudWatchAsyncClient::class.java))
        try {
            val sessions = registry.counter("http.sessions.total")
            sessions.increment()
            assertThat(sessions.count()).isEqualTo(1.0)
            val archive = registry.counter("arkistointi.errors.total")
            archive.increment()
            assertThat(archive.count()).isEqualTo(1.0)
            val unwanted = registry.counter("unexpected.metric")
            unwanted.increment()
            assertThat(unwanted.count()).isZero()
        } finally {
            registry.close()
        }
    }
}
