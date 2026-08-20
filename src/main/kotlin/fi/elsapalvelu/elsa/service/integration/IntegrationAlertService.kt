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
 * Suppresses duplicate operational alerts within one application instance until the
 * corresponding integration reports a successful operation.
 */
@Service
class IntegrationAlertService(
    private val alertPublisherService: AlertPublisherService
) {
    private val activeAlerts = ConcurrentHashMap.newKeySet<IntegrationAlertKey>()

    fun publishOnceUntilSuccess(key: IntegrationAlertKey, subject: String, message: String) {
        if (activeAlerts.add(key)) {
            alertPublisherService.publishAlert(subject, message)
        }
    }

    fun markSuccessful(key: IntegrationAlertKey) {
        activeAlerts.remove(key)
    }

    fun isActive(key: IntegrationAlertKey): Boolean = activeAlerts.contains(key)

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
}
