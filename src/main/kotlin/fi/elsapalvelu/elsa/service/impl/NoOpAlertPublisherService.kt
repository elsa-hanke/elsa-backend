package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.service.AlertPublisherService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

/**
 * No-op [AlertPublisherService] used when no SNS topic ARN is configured.
 *
 * This implementation is active in local development and test environments
 * where AWS SNS is not available.  It simply logs the alert at INFO level
 * so that developers can still observe what would have been sent.
 */
@Service
@ConditionalOnMissingBean(AlertPublisherService::class)
class NoOpAlertPublisherService : AlertPublisherService {

    private val log = LoggerFactory.getLogger(NoOpAlertPublisherService::class.java)

    override fun publishAlert(subject: String, message: String) {
        log.info("Alert (no-op – SNS topic ARN not configured): subject={}, message={}", subject, message)
    }
}

