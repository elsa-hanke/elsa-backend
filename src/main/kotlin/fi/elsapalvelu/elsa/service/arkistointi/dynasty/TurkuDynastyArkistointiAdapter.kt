package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

@Component
@Profile("!e2e-archive & !dynasty-approval-test")
class TurkuDynastyArkistointiAdapter(
    private val client: DynastyClient,
    private val metadata: TurkuDynastyMetadataMapper,
    private val ledger: DynastyStepLedger,
    private val mapper: ObjectMapper
) : ArkistointiAdapter {
    override val university = YliopistoEnum.TURUN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)

    override fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        client.validateConfiguration()
        metadata.validateConfiguration(caseType, configuration)
    }

    @Suppress("CyclomaticComplexMethod") // Keep the ordered resumable protocol visible in one place.
    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        val execution = pyynto.execution ?: return failure("DYNASTY_CONFIGURATION_INVALID")
        if (execution.jobId != pyynto.jobId || pyynto.university != university || pyynto.caseType !in supportedCaseTypes) return failure("DYNASTY_PAYLOAD_INVALID")
        val plan = try {
            client.validateConfiguration()
            metadata.plan(pyynto)
        } catch (e: DynastyFailure) { return failure(e.code) }
        catch (_: Exception) { return failure("DYNASTY_PAYLOAD_INVALID") }
        fun step(key: String, send: () -> String?): String? {
            when (val decision = ledger.begin(execution,key)) {
                is DynastyStepLedger.Decision.Done -> return validatedSavedId(key,decision.remoteId)
                DynastyStepLedger.Decision.InProgress -> throw DynastyFailure("DYNASTY_STEP_IN_PROGRESS", true)
                DynastyStepLedger.Decision.Unconfirmed -> throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
                DynastyStepLedger.Decision.Proceed -> Unit
            }
            val id = try { send() } catch (e: DynastyFailure) {
                if (e.definitelyNotProcessed) ledger.abandon(pyynto.jobId,key,execution.version)
                throw e
            }
            if (!ledger.complete(pyynto.jobId,key,execution.version,id)) throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
            return id
        }
        return try {
            val caseId = requireNotNull(step("CASE") { client.createCase(plan.tosClass,plan.caseJson) })
            val taskId = requireNotNull(step("TASK") { client.createTask(caseId,plan.task) })
            var mainId: String? = null
            pyynto.asiakirjat.forEachIndexed { index, doc ->
                val options = if (index == 0) mapOf("CaseSelector" to mapOf("Id" to caseId), "PhaseSelector" to mapOf("Id" to taskId))
                    else mapOf("caseSelector" to mapOf("id" to caseId.toLong()), "parentDocumentSelector" to mapOf("id" to requireNotNull(mainId).toLong()))
                val id = step("DOCUMENT:${doc.jarjestysnumero}") { client.createDocument(plan.documents[index],mapper.writeValueAsString(options),doc) }
                if (index == 0) mainId = id
            }
            // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
            step("ARCHIVE_CASE") { client.archiveCase(caseId); null }
            ArkistointiResult.Success(caseId)
        } catch (e: DynastyFailure) { failure(e.code,e.retryable) }
    }

    private fun validatedSavedId(key: String, id: String?): String? {
        if (key == "ARCHIVE_CASE") return null
        return DynastyId.requireValid(id, guid = key == "TASK")
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
