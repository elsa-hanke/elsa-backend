package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

enum class IntegrationAlertKey {
    SISU_TRE_OAUTH,
    SISU_TRE_API_AUTHENTICATION,
    PEPPI_TURKU_AUTHENTICATION,
    PEPPI_UEF_AUTHENTICATION,
    PEPPI_OULU_AUTHENTICATION,
    SISU_HY_AUTHENTICATION,
    SISU_HY_QUALIFICATION_EXPORT
}

/**
 * Suppresses duplicate operational alerts within one application instance.
 *
 * Authentication alerts are published immediately and remain active until a successful
 * operation. Connectivity alerts require consecutive failures for the same endpoint and
 * are rearmed only after consecutive successful connections.
 */
@Service
class IntegrationAlertService(
    private val alertPublisherService: AlertPublisherService
) {
    private val activeAlerts = ConcurrentHashMap.newKeySet<IntegrationAlertKey>()
    private val connectivityAlertStates =
        ConcurrentHashMap<IntegrationConnectivityAlertKey, IntegrationConnectivityAlertState>()

    fun publishOnceUntilSuccess(key: IntegrationAlertKey, subject: String, message: String) {
        if (activeAlerts.add(key)) {
            alertPublisherService.publishAlert(subject, message)
        }
    }

    fun markSuccessful(key: IntegrationAlertKey) {
        activeAlerts.remove(key)
    }

    fun isActive(key: IntegrationAlertKey): Boolean = activeAlerts.contains(key)

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
                    alertActive = alertActive
                )
            }
        }

        if (publishAlert) {
            alertPublisherService.publishAlert(subject, message)
        }
    }

    fun recordConnectivitySuccess(key: IntegrationAlertKey, endpoint: String) {
        val connectivityKey = IntegrationConnectivityAlertKey(key, endpoint)

        connectivityAlertStates.computeIfPresent(connectivityKey) { _, currentState ->
            if (!currentState.alertActive) {
                null
            } else {
                val consecutiveSuccesses = currentState.consecutiveSuccesses + 1
                if (consecutiveSuccesses >= CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                    null
                } else {
                    currentState.copy(
                        consecutiveFailures = 0,
                        consecutiveSuccesses = consecutiveSuccesses
                    )
                }
            }
        }
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
        internal const val CONNECTIVITY_FAILURE_ALERT_THRESHOLD = 5
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
    val alertActive: Boolean = false
)
