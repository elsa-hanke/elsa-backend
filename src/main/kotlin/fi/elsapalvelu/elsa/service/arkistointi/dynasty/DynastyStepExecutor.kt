package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.springframework.stereotype.Component

/**
 * Runs one resumable mutation. Ledger calls keep their own REQUIRES_NEW transactions;
 * the external request runs between the committed intent and completion.
 */
@Component
class DynastyStepExecutor(private val ledger: DynastyStepLedger, properties: ApplicationProperties) {
    private val settings = properties.getArkistointi().getTurku().dynasty

    fun <T> execute(execution: ArkistointiJobExecution, key: DynastyStepKey<T>, send: () -> T): T {
        when (val decision = ledger.begin(execution,key,settings.inProgressGrace)) {
            is DynastyStepLedger.Decision.Done -> return key.responseKind.fromSaved(decision.remoteId)
            DynastyStepLedger.Decision.InProgress -> throw DynastyFailure(DynastyFailureCategory.STEP_IN_PROGRESS)
            DynastyStepLedger.Decision.Unconfirmed -> throw DynastyFailure(DynastyFailureCategory.DELIVERY_UNCONFIRMED)
            DynastyStepLedger.Decision.Proceed -> Unit
        }
        val id = try { send() } catch (e: DynastyFailure) {
            if (e.category.definitelyNotProcessed) ledger.abandon(execution.jobId,key,execution.version)
            throw e
        }
        if (!ledger.complete(execution.jobId,key,execution.version,key.responseKind.toSaved(id))) throw DynastyFailure(DynastyFailureCategory.DELIVERY_UNCONFIRMED)
        return id
    }

}
