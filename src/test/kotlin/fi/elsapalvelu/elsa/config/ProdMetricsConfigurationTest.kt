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
            registry.gauge("http.sessions.active", java.util.concurrent.atomic.AtomicInteger(3))
            assertThat(registry.find("http.sessions.active").gauge()?.value()).isEqualTo(3.0)
            registry.counter("arkistointi.errors.total").increment()
            assertThat(registry.find("arkistointi.errors.total").counter()).isNotNull()
            registry.counter("unexpected.metric").increment()
            assertThat(registry.find("unexpected.metric").counter()).isNull()

        } finally {
            registry.close()
        }
    }
}
