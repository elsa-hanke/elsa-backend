package fi.elsapalvelu.elsa.config

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

/**
 * Provides a [CloudWatchAsyncClient] bean configured via Spring Cloud AWS's
 * [AwsClientBuilderConfigurer], ensuring Micrometer's CloudWatch meter-registry uses the
 * correct region (`spring.cloud.aws.region.static`) and the ECS task-role credentials.
 *
 * **Why this is needed:**
 * Micrometer's [io.micrometer.cloudwatch2.CloudWatchMeterRegistry] auto-configuration
 * (`@ConditionalOnMissingBean(CloudWatchAsyncClient.class)`) falls back to
 * `CloudWatchAsyncClient.create()` when no bean is present.  In AWS Fargate this call
 * relies on the SDK's region-provider chain, which does NOT read
 * `spring.cloud.aws.region.static` and requires `AWS_REGION` / `AWS_DEFAULT_REGION` to be
 * set as an environment variable — variables that Fargate does not inject automatically
 * (unlike Lambda).  Without a resolved region the async client is created but all
 * `PutMetricData` calls fail silently, so no metrics appear in CloudWatch.
 *
 * By declaring this bean here (ahead of Micrometer's auto-configuration), we hand
 * Micrometer an already-configured client and the problem is avoided entirely.
 *
 * Active only when `management.cloudwatch2.metrics.export.enabled=true` so it is a no-op
 * in development / test where CloudWatch export is disabled.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CloudWatchAsyncClient::class)
@ConditionalOnProperty(
    prefix = "management.cloudwatch2.metrics.export",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class CloudWatchMeterRegistryConfiguration {

    private val log = LoggerFactory.getLogger(CloudWatchMeterRegistryConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean(CloudWatchAsyncClient::class)
    fun cloudWatchAsyncClient(configurer: AwsClientBuilderConfigurer): CloudWatchAsyncClient {
        log.info("Creating CloudWatchAsyncClient via Spring Cloud AWS AwsClientBuilderConfigurer")
        return configurer.configure(CloudWatchAsyncClient.builder()).build()
    }
}

