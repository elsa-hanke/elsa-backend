package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Runs one resumable mutation. Ledger calls keep their own REQUIRES_NEW transactions;
 * the external request runs between the committed intent and completion.
 */
@Component
class DynastyStepExecutor(private val ledger: DynastyStepLedger) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> execute(
        execution: ArkistointiJobExecution, grace: Duration, key: DynastyStepKey<T>, send: () -> T
    ): T {
        val decision = ledger.begin(execution, key, grace)
        if (decision != DynastyStepLedger.Decision.Proceed) return existingResult(execution, key, decision)
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

    internal fun <T> existing(execution: ArkistointiJobExecution, grace: Duration, key: DynastyStepKey<T>): T? =
        ledger.existing(execution, key, grace)?.let { existingResult(execution, key, it) }

    private fun <T> existingResult(
        execution: ArkistointiJobExecution, key: DynastyStepKey<T>, decision: DynastyStepLedger.Decision
    ): T = when (decision) {
        is DynastyStepLedger.Decision.Done -> {
            log.info("Dynasty step reused from ledger [jobId={}, step={}, remoteId={}]", execution.jobId, key.databaseKey, decision.remoteId)
            key.responseKind.fromSaved(decision.remoteId)
        }
        DynastyStepLedger.Decision.InProgress -> throw failed(execution, key, DynastyFailureCategory.STEP_IN_PROGRESS)
        DynastyStepLedger.Decision.Unconfirmed -> throw failed(execution, key, DynastyFailureCategory.DELIVERY_UNCONFIRMED)
        DynastyStepLedger.Decision.Proceed -> error("Expected an existing Dynasty step")
    }

    private fun failed(execution: ArkistointiJobExecution, key: DynastyStepKey<*>, category: DynastyFailureCategory): DynastyFailure {
        log.error("Dynasty step failed [jobId={}, step={}, category={}]", execution.jobId, key.databaseKey, category)
        return DynastyFailure(category)
    }

}
