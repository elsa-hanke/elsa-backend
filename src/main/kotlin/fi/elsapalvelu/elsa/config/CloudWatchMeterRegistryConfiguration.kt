package fi.elsapalvelu.elsa.config

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer
import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
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

/**
 * Manually wires the Micrometer CloudWatch2 meter-registry.
 *
 * **Why this is needed:**
 * Spring Boot 3.5.x has NO built-in autoconfiguration for CloudWatch2 metrics export
 * (unlike Datadog, Graphite, InfluxDB etc.). The `micrometer-registry-cloudwatch2` jar
 * contains only the registry implementation — it ships no Spring Boot `@AutoConfiguration`.
 * This means:
 *  - `management.cloudwatch2.metrics.export.*` properties are bound by nobody
 *  - `CloudWatchMeterRegistry` is never instantiated unless we do it here
 *  - `CloudWatchAsyncClient` falls back to SDK default region-provider chain which
 *    fails on ECS Fargate (no `AWS_REGION` env variable auto-injected by Fargate)
 *
 * We therefore create all three beans ourselves:
 *  1. [CloudWatchConfig]        – bridges Spring's `management.cloudwatch2.metrics.export.*`
 *                                 properties to Micrometer's `cloudwatch.*` key format
 *  2. [CloudWatchAsyncClient]   – built via Spring Cloud AWS [AwsClientBuilderConfigurer]
 *                                 so it respects `spring.cloud.aws.region.static`
 *  3. [CloudWatchMeterRegistry] – the actual Micrometer registry; marked `@Lazy(false)` so
 *                                 its internal [java.util.concurrent.ScheduledExecutorService]
 *                                 starts at boot even when `spring.main.lazy-initialization=true`
 *
 * Active only when `management.cloudwatch2.metrics.export.enabled=true`.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CloudWatchAsyncClient::class, CloudWatchMeterRegistry::class)
@ConditionalOnProperty(
    prefix = "management.cloudwatch2.metrics.export",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class CloudWatchMeterRegistryConfiguration(
    private val environment: Environment
) {

    private val log = LoggerFactory.getLogger(CloudWatchMeterRegistryConfiguration::class.java)

    /**
     * Bridges Spring's `management.cloudwatch2.metrics.export.*` YAML properties to the
     * Micrometer [CloudWatchConfig] interface.
     *
     * Micrometer calls `get("cloudwatch.namespace")`, `get("cloudwatch.step")`, etc.
     * We strip the `cloudwatch.` prefix and look up Spring's property:
     * `management.cloudwatch2.metrics.export.<suffix>`.
     */
    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchConfig::class)
    fun cloudWatchConfig(): CloudWatchConfig {
        return CloudWatchConfig { key ->
            // key format: "cloudwatch.namespace", "cloudwatch.step", "cloudwatch.batchSize" …
            val suffix = key.removePrefix("cloudwatch.")
            environment.getProperty("management.cloudwatch2.metrics.export.$suffix")
        }
    }

    /**
     * [CloudWatchAsyncClient] configured via Spring Cloud AWS so it picks up
     * `spring.cloud.aws.region.static` instead of relying on the SDK region-provider
     * chain (which fails on ECS Fargate where `AWS_REGION` is not auto-injected).
     */
    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchAsyncClient::class)
    fun cloudWatchAsyncClient(configurer: AwsClientBuilderConfigurer): CloudWatchAsyncClient {
        log.info("Creating CloudWatchAsyncClient via Spring Cloud AWS AwsClientBuilderConfigurer")
        return configurer.configure(CloudWatchAsyncClient.builder()).build()
    }

    /**
     * The Micrometer [CloudWatchMeterRegistry].
     *
     * `@Lazy(false)` is critical: with `spring.main.lazy-initialization=true` (dev profile)
     * all beans are lazy by default.  If this bean is lazy its internal
     * [java.util.concurrent.ScheduledExecutorService] never starts and no metrics are ever
     * published.  Forcing eager initialization ensures the scheduler is running from
     * application startup.
     *
     * Spring Boot's [io.micrometer.core.instrument.composite.CompositeMeterRegistry]
     * auto-configuration picks up ALL [io.micrometer.core.instrument.MeterRegistry] beans
     * via `ObjectProvider`, so this registry is automatically included in the composite
     * and receives all JVM / HTTP / custom meters.
     */
    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(CloudWatchMeterRegistry::class)
    fun cloudWatchMeterRegistry(
        config: CloudWatchConfig,
        clock: Clock,
        client: CloudWatchAsyncClient
    ): CloudWatchMeterRegistry {
        log.info(
            "Creating CloudWatchMeterRegistry – namespace='{}', step={}",
            config.namespace(),
            config.step()
        )
        return CloudWatchMeterRegistry(config, clock, client)
    }

    /**
     * Logs CloudWatch export configuration once the application is fully started.
     *
     * This fires AFTER context refresh, so the message appears in the tail of startup logs
     * (after "Application is running!") and confirms the registry is active.
     *
     * NOTE: Spring Cloud AWS 3.x ships its own [io.awspring.cloud.autoconfigure.metrics.CloudWatchExportAutoConfiguration]
     * which is enabled by default (matchIfMissing=true on spring.cloud.aws.cloudwatch.enabled).
     * Because this class is a regular @Configuration (not @AutoConfiguration) it is processed
     * FIRST, so all three beans below are created before Spring Cloud AWS's auto-config can see
     * them. The Spring Cloud AWS auto-config then finds all beans present and skips them
     * (@ConditionalOnMissingBean on each of its factory methods).
     */
    @EventListener(ApplicationReadyEvent::class)
    fun logCloudWatchConfig() {
        val namespace = environment.getProperty("management.cloudwatch2.metrics.export.namespace")
        val step = environment.getProperty("management.cloudwatch2.metrics.export.step")
        log.info(
            "CloudWatch metrics export is ACTIVE – namespace='{}', step={}",
            namespace,
            step
        )
    }
}
