package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Runs one resumable mutation. Ledger calls keep their own REQUIRES_NEW transactions;
 * the external request runs between the committed intent and completion.
 */
@Component
class DynastyStepExecutor(private val ledger: DynastyStepLedger, properties: ApplicationProperties) {
    private val archiveSettings = properties.getArkistointi()
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> execute(
        execution: ArkistointiJobExecution, university: YliopistoEnum, key: DynastyStepKey<T>, send: () -> T
    ): T {
        val grace = when (university) {
            YliopistoEnum.TURUN_YLIOPISTO -> archiveSettings.getTurku().dynasty.inProgressGrace
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> archiveSettings.getUef().dynasty.inProgressGrace
            else -> throw IllegalArgumentException("Unsupported Dynasty university")
        }
        when (val decision = ledger.begin(execution, key, grace)) {
            is DynastyStepLedger.Decision.Done -> {
                log.info("Dynasty step reused from ledger [jobId={}, step={}, remoteId={}]", execution.jobId, key.databaseKey, decision.remoteId)
                return key.responseKind.fromSaved(decision.remoteId)
            }
            DynastyStepLedger.Decision.InProgress -> throw failed(execution, key, DynastyFailureCategory.STEP_IN_PROGRESS)
            DynastyStepLedger.Decision.Unconfirmed -> throw failed(execution, key, DynastyFailureCategory.DELIVERY_UNCONFIRMED)
            DynastyStepLedger.Decision.Proceed -> Unit
        }
        val id = try { send() } catch (e: DynastyFailure) {
            failed(execution, key, e.category)
            if (e.category.definitelyNotProcessed) ledger.abandon(execution.jobId,key,execution.version)
            throw e
        }
        val savedId = key.responseKind.toSaved(id)
        if (!ledger.complete(execution.jobId,key,execution.version,savedId)) throw failed(execution, key, DynastyFailureCategory.DELIVERY_UNCONFIRMED)
        log.info("Dynasty step executed [jobId={}, step={}, remoteId={}]", execution.jobId, key.databaseKey, savedId)
        return id
    }

    private fun failed(execution: ArkistointiJobExecution, key: DynastyStepKey<*>, category: DynastyFailureCategory): DynastyFailure {
        log.error("Dynasty step failed [jobId={}, step={}, category={}]", execution.jobId, key.databaseKey, category)
        return DynastyFailure(category)
    }

}
