package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

@Component
@Profile("!e2e-archive")
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
        val plan = try {
            validateSubmission(pyynto)
            client.validateConfiguration()
            metadata.plan(pyynto)
        } catch (e: DynastyFailure) {
            return failure(e.code)
        }
        catch (_: Exception) {
            return failure("DYNASTY_PAYLOAD_INVALID")
        }
        return try {
            val caseId = deliver(pyynto, plan)
            ArkistointiResult.Success(caseId.toString())
        } catch (e: DynastyFailure) {
            failure(e.code, e.retryable)
        }
    }

    private fun validateSubmission(submission: ArkistointiSubmission) {
        require(submission.execution.jobId == submission.jobId)
        require(submission.university == university && submission.caseType in supportedCaseTypes)
    }

    private fun deliver(submission: ArkistointiSubmission, plan: TurkuDynastyMetadataMapper.Plan): DynastyCaseId {
        val execution = submission.execution
        val caseId = steps.execute(execution, DynastyStepKey.Case, client.inProgressGrace) {
            client.createCase(plan.tosClass, plan.caseRequest)
        }
        val taskId = steps.execute(execution, DynastyStepKey.Task, client.inProgressGrace) {
            client.createTask(caseId, plan.task)
        }
        deliverDocuments(submission, plan, caseId, taskId)
        // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
        steps.execute(execution, DynastyStepKey.ArchiveCase, client.inProgressGrace) { client.archiveCase(caseId) }
        return caseId
    }

    private fun deliverDocuments(
        submission: ArkistointiSubmission, plan: TurkuDynastyMetadataMapper.Plan,
        caseId: DynastyCaseId, taskId: DynastyTaskId
    ) {
        val mainId = deliverDocument(
            submission.execution, submission.asiakirjat.first(), plan.documents.first(), DynastyMainDocumentOptions(caseId, taskId)
        )
        submission.asiakirjat.drop(1).forEachIndexed { index, document ->
            deliverDocument(
                submission.execution, document, plan.documents[index + 1], DynastyAttachmentOptions(caseId, mainId)
            )
        }
    }

    private fun deliverDocument(
        execution: ArkistointiJobExecution, document: LoadedArkistointiAsiakirja,
        metadata: DynastyDocumentMetadata, options: DynastyDocumentOptions
    ): DynastyDocumentId = steps.execute(execution, DynastyStepKey.Document(document.jarjestysnumero), client.inProgressGrace) {
        client.createDocument(metadata, options, document)
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
