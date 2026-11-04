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
    fun execute(execution: ArkistointiJobExecution, key: DynastyStepKey, inProgressGrace: Duration, send: () -> String?): String? {
        when (val decision = ledger.begin(execution,key,inProgressGrace)) {
            is DynastyStepLedger.Decision.Done -> return validatedSavedId(key,decision.remoteId)
            DynastyStepLedger.Decision.InProgress -> throw DynastyFailure("DYNASTY_STEP_IN_PROGRESS", true)
            DynastyStepLedger.Decision.Unconfirmed -> throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
            DynastyStepLedger.Decision.Proceed -> Unit
        }
        val id = try { send() } catch (e: DynastyFailure) {
            if (e.definitelyNotProcessed) ledger.abandon(execution.jobId,key,execution.version)
            throw e
        }
        if (!ledger.complete(execution.jobId,key,execution.version,id)) throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
        return id
    }

    private fun validatedSavedId(key: DynastyStepKey, id: String?): String? = when (key) {
        DynastyStepKey.ArchiveCase -> null
        DynastyStepKey.Task -> DynastyId.requireValid(id, guid = true)
        DynastyStepKey.Case, is DynastyStepKey.Document -> DynastyId.requireValid(id, guid = false)
    }

}
