package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

enum class IntegrationAlertKey {
    SISU_TRE_OAUTH,
    SISU_TRE_API_AUTHENTICATION,
    SISU_TRE_INTERNAL_SERVER_ERROR,
    PEPPI_TURKU_AUTHENTICATION,
    PEPPI_UEF_AUTHENTICATION,
    PEPPI_OULU_AUTHENTICATION,
    PEPPI_OULU_INTERNAL_SERVER_ERROR,
    SISU_HY_AUTHENTICATION,
    SISU_HY_QUALIFICATION_EXPORT
}

/**
 * Suppresses duplicate operational alerts within one application instance, and publishes a
 * follow-up "recovered" (OK) alert once the underlying problem goes away, so an operator
 * monitoring the SNS topic can see that an active incident is over without having to guess.
 *
 * Authentication alerts are published immediately and remain active until a successful
 * operation. Connectivity alerts require consecutive failures for the same endpoint and
 * are rearmed only after consecutive successful connections.
 */
@Service
class IntegrationAlertService(
    private val alertPublisherService: AlertPublisherService
) {
    // Maps an active alert key to the subject it was originally published with, so the
    // recovery alert can reference it.
    private val activeAlerts = ConcurrentHashMap<IntegrationAlertKey, String>()
    private val connectivityAlertStates =
        ConcurrentHashMap<IntegrationConnectivityAlertKey, IntegrationConnectivityAlertState>()

    fun publishOnceUntilSuccess(key: IntegrationAlertKey, subject: String, message: String) {
        if (activeAlerts.putIfAbsent(key, subject) == null) {
            alertPublisherService.publishAlert(subject, message)
        }
    }

    fun markSuccessful(key: IntegrationAlertKey) {
        val previousSubject = activeAlerts.remove(key)
        if (previousSubject != null) {
            publishRecoveryAlert(previousSubject)
        }
    }

    fun isActive(key: IntegrationAlertKey): Boolean = activeAlerts.containsKey(key)

    fun recordConnectivityFailure(
        key: IntegrationAlertKey,
        endpoint: String,
        subject: String,
        message: String
    ) {
        val connectivityKey = IntegrationConnectivityAlertKey(key, endpoint)
        var publishAlert = false

        connectivityAlertStates.compute(connectivityKey) { _, currentState ->
            if (currentState?.alertActive == true) {
                currentState.copy(consecutiveSuccesses = 0)
            } else {
                val consecutiveFailures = (currentState?.consecutiveFailures ?: 0) + 1
                val alertActive = consecutiveFailures >= CONNECTIVITY_FAILURE_ALERT_THRESHOLD
                publishAlert = alertActive
                IntegrationConnectivityAlertState(
                    consecutiveFailures = consecutiveFailures,
                    alertActive = alertActive,
                    subject = if (alertActive) subject else null
                )
            }
        }

        if (publishAlert) {
            alertPublisherService.publishAlert(subject, message)
        }
    }

    fun recordConnectivitySuccess(key: IntegrationAlertKey, endpoint: String) {
        val connectivityKey = IntegrationConnectivityAlertKey(key, endpoint)
        var recoveredAlertSubject: String? = null

        connectivityAlertStates.computeIfPresent(connectivityKey) { _, currentState ->
            if (!currentState.alertActive) {
                null
            } else {
                val consecutiveSuccesses = currentState.consecutiveSuccesses + 1
                if (consecutiveSuccesses >= CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                    recoveredAlertSubject = currentState.subject
                    null
                } else {
                    currentState.copy(
                        consecutiveFailures = 0,
                        consecutiveSuccesses = consecutiveSuccesses
                    )
                }
            }
        }

        recoveredAlertSubject?.let { subject ->
            publishRecoveryAlert(subject, endpoint)
        }
    }

    private fun publishRecoveryAlert(originalSubject: String, endpoint: String? = null) {
        val endpointSuffix = endpoint?.let { " Endpoint: $it." }.orEmpty()
        alertPublisherService.publishAlert(
            "$originalSubject - tilanne korjaantunut",
            "Aiemmin ilmoitettu häiriö ($originalSubject) on korjaantunut, " +
                "integraatio toimii jälleen normaalisti.$endpointSuffix"
        )
    }

    fun updateGraphQlAuthentication(
        key: IntegrationAlertKey,
        integrationName: String,
        authenticated: Boolean
    ) {
        if (authenticated) {
            markSuccessful(key)
        } else {
            publishOnceUntilSuccess(
                key,
                "$integrationName opintotietointegraation autentikointi epäonnistui",
                "$integrationName palautti GraphQL UNAUTHENTICATED -virheen. " +
                    "Virhe ei liity yksittäiseen henkilötunnukseen."
            )
        }
    }

    companion object {
        internal const val CONNECTIVITY_FAILURE_ALERT_THRESHOLD = 10
        internal const val CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD = 3
    }
}

private data class IntegrationConnectivityAlertKey(
    val integrationAlertKey: IntegrationAlertKey,
    val endpoint: String
)

private data class IntegrationConnectivityAlertState(
    val consecutiveFailures: Int = 0,
    val consecutiveSuccesses: Int = 0,
    val alertActive: Boolean = false,
    val subject: String? = null
)
