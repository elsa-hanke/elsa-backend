package fi.elsapalvelu.elsa.config

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer
import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.config.MeterFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient


@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CloudWatchAsyncClient::class, CloudWatchMeterRegistry::class)
@ConditionalOnProperty(
    prefix = "management.cloudwatch2.metrics.export",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class CloudWatchMeterRegistryConfiguration(private val environment: Environment) {

    private val log = LoggerFactory.getLogger(CloudWatchMeterRegistryConfiguration::class.java)

    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchConfig::class)
    fun cloudWatchConfig(): CloudWatchConfig {
        return CloudWatchConfig { key ->
            val suffix = key.removePrefix("cloudwatch.")
            environment.getProperty("management.cloudwatch2.metrics.export.$suffix")
        }
    }

    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchAsyncClient::class)
    fun cloudWatchAsyncClient(configurer: AwsClientBuilderConfigurer): CloudWatchAsyncClient {
        log.info("Creating CloudWatchAsyncClient via Spring Cloud AWS AwsClientBuilderConfigurer")
        return configurer.configure(CloudWatchAsyncClient.builder()).build()
    }

    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchMeterRegistry::class)
    fun cloudWatchMeterRegistry(config: CloudWatchConfig, clock: Clock, client: CloudWatchAsyncClient): CloudWatchMeterRegistry {
        log.info("Creating CloudWatchMeterRegistry – namespace='{}', step={}", config.namespace(), config.step())
        val registry = CloudWatchMeterRegistry(config, clock, client)

        val allowedNames = resolveAllowedMetricNames()
        if (allowedNames.isNotEmpty()) {
            log.info("CloudWatch metric filter active – {} metrics allowed: {}", allowedNames.size, allowedNames.sorted().joinToString(", "))
            registry.config().meterFilter(MeterFilter.denyUnless { id -> id.name in allowedNames })
        } else {
            log.warn("CloudWatch metric filter is NOT configured (management.cloudwatch2.metrics.filter.allowed-names is empty). " +
                    "ALL metrics will be exported — this may generate a lot of noise and cost.")
        }

        return registry
    }

    private fun resolveAllowedMetricNames(): Set<String> {
        val raw = environment.getProperty("management.cloudwatch2.metrics.filter.allowed-names") ?: return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun logCloudWatchConfig() {
        val namespace = environment.getProperty("management.cloudwatch2.metrics.export.namespace")
        val step = environment.getProperty("management.cloudwatch2.metrics.export.step")
        log.info("CloudWatch metrics export is ACTIVE – namespace='{}', step={}", namespace, step)
    }
}
