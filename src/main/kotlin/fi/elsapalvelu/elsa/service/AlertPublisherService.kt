package fi.elsapalvelu.elsa.service

/**
 * Service for publishing operational alerts to an external notification channel.
 *
 * The service is intentionally unaware of the delivery mechanism (SNS, email, etc.).
 * Callers simply describe what went wrong and this service forwards the alert.
 */
interface AlertPublisherService {

    /**
     * Publish an alert with the given [subject] and [message].
     *
     * Implementations must never throw – a failed alert delivery should be
     * logged internally but must not disrupt the calling operation.
     */
    fun publishAlert(subject: String, message: String)
}

