package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.AlertPublisherService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

/**
 * [AlertPublisherService] implementation that publishes to SNS when configured,
 * and silently falls back to a log-only no-op otherwise.
 *
 * **Why `ObjectProvider<SnsClient>` instead of conditional beans:**
 * Using `@ConditionalOnMissingBean` / `@ConditionalOnProperty` on `@Service` beans is
 * unreliable because Spring's component-scan ordering is non-deterministic – the condition
 * may be evaluated before or after other beans are registered.  `ObjectProvider<T>` is
 * Spring's built-in mechanism for *optional* dependencies: a provider is always injected
 * successfully even when `T` has no registered bean, and `getIfAvailable()` returns `null`
 * in that case.  This gives us a single, always-registered bean with zero conditional magic.
 *
 * **Behaviour at runtime:**
 * - `application.alert.sns-topic-arn` empty/absent → logs info, returns  (dev / test)
 * - `SnsClient` not available (AWS disabled in tests) → logs warning, returns
 * - Both present → publishes SNS notification → Lambda forwards to Slack
 */
@Service
class AlertPublisherServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val snsClientProvider: ObjectProvider<SnsClient>
) : AlertPublisherService {

    private val log = LoggerFactory.getLogger(AlertPublisherServiceImpl::class.java)

    override fun publishAlert(subject: String, message: String) {
        val topicArn = applicationProperties.getAlert().snsTopicArn
        if (topicArn.isNullOrBlank()) {
            log.info("Alert not sent (SNS topic ARN not configured): subject={}", subject)
            return
        }

        val snsClient = snsClientProvider.getIfAvailable()
        if (snsClient == null) {
            log.warn("Alert not sent (SnsClient bean not available): subject={}", subject)
            return
        }

        try {
            snsClient.publish(
                PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject.take(100)) // SNS subject max is 100 chars
                    .message(message)
                    .build()
            )
            log.info("Alert published to SNS: subject={}", subject)
        } catch (e: Exception) {
            log.error("Failed to publish alert to SNS: subject={}", subject, e)
        }
    }
}

