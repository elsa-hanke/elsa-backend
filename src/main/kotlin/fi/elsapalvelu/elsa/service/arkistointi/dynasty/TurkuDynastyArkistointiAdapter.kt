package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

@Component
@Profile("!e2e-archive & !dynasty-approval-test")
class TurkuDynastyArkistointiAdapter(
    @Qualifier("turkuDynastyClient") private val client: DynastyClient,
    private val metadata: TurkuDynastyMetadataMapper,
    private val steps: DynastyStepExecutor
) : ArkistointiAdapter {
    override val canResumeAfterSuccessPersistenceFailure = true
    override val university = YliopistoEnum.TURUN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)

    override fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        client.validateConfiguration()
        metadata.validateConfiguration(caseType, configuration)
    }

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        val execution = pyynto.execution
        if (execution.jobId != pyynto.jobId || pyynto.university != university || pyynto.caseType !in supportedCaseTypes) return failure("DYNASTY_PAYLOAD_INVALID")
        val plan = try {
            client.validateConfiguration()
            metadata.plan(pyynto)
        } catch (e: DynastyFailure) { return failure(e.code) }
        catch (_: Exception) { return failure("DYNASTY_PAYLOAD_INVALID") }
        return try {
            val caseId = steps.execute(execution, DynastyStepKey.Case, client.inProgressGrace) {
                client.createCase(plan.tosClass,plan.caseRequest)
            }
            val taskId = steps.execute(execution, DynastyStepKey.Task, client.inProgressGrace) {
                client.createTask(caseId,plan.task)
            }
            var mainId: DynastyDocumentId? = null
            pyynto.asiakirjat.forEachIndexed { index, doc ->
                val options = if (index == 0) DynastyMainDocumentOptions(caseId, taskId)
                    else DynastyAttachmentOptions(caseId, requireNotNull(mainId))
                val id = steps.execute(execution, DynastyStepKey.Document(doc.jarjestysnumero), client.inProgressGrace) {
                    client.createDocument(plan.documents[index],options,doc)
                }
                if (index == 0) mainId = id
            }
            // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
            steps.execute(execution, DynastyStepKey.ArchiveCase, client.inProgressGrace) { client.archiveCase(caseId) }
            ArkistointiResult.Success(caseId.toString())
        } catch (e: DynastyFailure) { failure(e.code,e.retryable) }
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
