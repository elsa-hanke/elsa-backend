package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ArkistointiJobObserver(
    private val metrics: ArkistointiMetricsService,
    private val alerts: AlertPublisherService,
    properties: ApplicationProperties
) {
    private val alertAfterAttempts = properties.getArkistointi().retry.also { it.validate() }.alertAfterAttempts
    private val log = LoggerFactory.getLogger(javaClass)

    /** Best effort after commit: telemetry must never turn a committed delivery into a scheduler failure. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCompleted(event: ArkistointiAttemptCompleted) {
        try {
            metrics.recordAsyncAttempt(event.university, event.caseType, event.outcome.name)
            if (event.outcome == ArkistointiAttemptCompleted.Outcome.SUCCESS) {
                metrics.recordSuccess(event.university, event.caseType)
            } else {
                metrics.recordError(event.university, event.caseType)
            }
        } catch (_: Exception) {
            log.warn("Archive metrics publication failed [jobId={}]", event.jobId)
        }
        val code = if (event.outcome == ArkistointiAttemptCompleted.Outcome.SUCCESS) "NONE"
            else event.errorCode?.takeIf { SAFE_CODE.matches(it) } ?: "ARCHIVE_ERROR_UNSPECIFIED"
        val details = "jobId=${event.jobId}, university=${event.university.name}, caseType=${event.caseType.value}, " +
            "attempt=${event.attempt}, outcome=${event.outcome.name}, errorCode=$code"
        when {
            event.outcome == ArkistointiAttemptCompleted.Outcome.TERMINAL_FAILURE -> {
                log.error("Archive job requires intervention [{}]", details)
                publish("Asynkroninen arkistointi vaatii toimenpiteitä", "Automaattiset yritykset on lopetettu. $details", event)
            }
            event.outcome == ArkistointiAttemptCompleted.Outcome.RETRYABLE_FAILURE && event.attempt == alertAfterAttempts -> {
                log.warn("Archive delivery delayed [{}]", details)
                publish("Asynkroninen arkistointi viivästyy", "Automaattiset yritykset jatkuvat. $details", event)
            }
            else -> log.info("Archive attempt committed [{}]", details)
        }
    }

    private fun publish(subject: String, message: String, event: ArkistointiAttemptCompleted) {
        try {
            alerts.publishAlert(subject, message)
        } catch (_: Exception) {
            log.warn("Archive alert publication failed [jobId={}]", event.jobId)
        }
    }

    private companion object {
        val SAFE_CODE = Regex("[A-Z][A-Z0-9_]{0,99}")
    }
}
