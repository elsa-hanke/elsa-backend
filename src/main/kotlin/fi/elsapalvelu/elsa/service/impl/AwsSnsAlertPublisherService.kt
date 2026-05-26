package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.AlertPublisherService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

/**
 * SNS-backed [AlertPublisherService].
 *
 * Active only when `application.alert.sns-topic-arn` is configured (non-empty).
 * Publishes a structured SNS notification so that any downstream subscription
 * (AWS Chatbot → Slack, email, SMS…) can deliver it without changes to this service.
 *
 * Delivery failures are caught and logged to prevent cascading failures in the
 * calling business operation.
 */
@Service
@ConditionalOnProperty(name = ["application.alert.sns-topic-arn"], matchIfMissing = false)
class AwsSnsAlertPublisherService(
    private val snsClient: SnsClient,
    private val applicationProperties: ApplicationProperties
) : AlertPublisherService {

    private val log = LoggerFactory.getLogger(AwsSnsAlertPublisherService::class.java)

    override fun publishAlert(subject: String, message: String) {
        val topicArn = applicationProperties.getAlert().snsTopicArn
        if (topicArn.isNullOrBlank()) {
            log.warn("SNS topic ARN is not configured – alert skipped: subject={}", subject)
            return
        }
        try {
            snsClient.publish(
                PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject.take(100)) // SNS subject max length is 100 characters
                    .message(message)
                    .build()
            )
            log.info("Alert published to SNS: subject={}", subject)
        } catch (e: Exception) {
            log.error("Failed to publish alert to SNS: subject={}", subject, e)
        }
    }
}

