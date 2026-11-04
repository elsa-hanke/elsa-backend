package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val steps: DynastyStepExecutor,
    private val mapper: ObjectMapper
) : ArkistointiAdapter {
    override val canResumeAfterSuccessPersistenceFailure = true
    override val university = YliopistoEnum.TURUN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)

    override fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        client.validateConfiguration()
        metadata.validateConfiguration(caseType, configuration)
    }

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        val execution = pyynto.execution ?: return failure("DYNASTY_CONFIGURATION_INVALID")
        if (execution.jobId != pyynto.jobId || pyynto.university != university || pyynto.caseType !in supportedCaseTypes) return failure("DYNASTY_PAYLOAD_INVALID")
        val plan = try {
            client.validateConfiguration()
            metadata.plan(pyynto)
        } catch (e: DynastyFailure) { return failure(e.code) }
        catch (_: Exception) { return failure("DYNASTY_PAYLOAD_INVALID") }
        return try {
            val caseId = requireNotNull(steps.execute(execution, DynastyStepKey.Case, client.inProgressGrace) {
                client.createCase(plan.tosClass,plan.caseJson)
            })
            val taskId = requireNotNull(steps.execute(execution, DynastyStepKey.Task, client.inProgressGrace) {
                client.createTask(caseId,plan.task)
            })
            var mainId: String? = null
            pyynto.asiakirjat.forEachIndexed { index, doc ->
                val options = if (index == 0) mapOf("CaseSelector" to mapOf("Id" to caseId), "PhaseSelector" to mapOf("Id" to taskId))
                    else mapOf("caseSelector" to mapOf("id" to caseId.toLong()), "parentDocumentSelector" to mapOf("id" to requireNotNull(mainId).toLong()))
                val id = steps.execute(execution, DynastyStepKey.Document(doc.jarjestysnumero), client.inProgressGrace) {
                    client.createDocument(plan.documents[index],mapper.writeValueAsString(options),doc)
                }
                if (index == 0) mainId = id
            }
            // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
            steps.execute(execution, DynastyStepKey.ArchiveCase, client.inProgressGrace) { client.archiveCase(caseId); null }
            ArkistointiResult.Success(caseId)
        } catch (e: DynastyFailure) { failure(e.code,e.retryable) }
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
