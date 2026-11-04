package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Runs one resumable mutation. Ledger calls keep their own REQUIRES_NEW transactions;
 * the external request runs between the committed intent and completion.
 */
@Component
class DynastyStepExecutor(private val ledger: DynastyStepLedger) {
    fun <T> execute(execution: ArkistointiJobExecution, key: DynastyStepKey<T>, inProgressGrace: Duration, send: () -> T): T {
        when (val decision = ledger.begin(execution,key,inProgressGrace)) {
            is DynastyStepLedger.Decision.Done -> return key.responseKind.fromSaved(decision.remoteId)
            DynastyStepLedger.Decision.InProgress -> throw DynastyFailure("DYNASTY_STEP_IN_PROGRESS", true)
            DynastyStepLedger.Decision.Unconfirmed -> throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
            DynastyStepLedger.Decision.Proceed -> Unit
        }
        val id = try { send() } catch (e: DynastyFailure) {
            if (e.definitelyNotProcessed) ledger.abandon(execution.jobId,key,execution.version)
            throw e
        }
        if (!ledger.complete(execution.jobId,key,execution.version,key.responseKind.toSaved(id))) throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
        return id
    }

}
