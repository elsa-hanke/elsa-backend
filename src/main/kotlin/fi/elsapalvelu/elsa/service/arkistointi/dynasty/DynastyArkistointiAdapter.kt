package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

open class DynastyArkistointiAdapter internal constructor(
    final override val university: YliopistoEnum,
    private val client: DynastyClient,
    private val metadata: DynastyMetadataMapper,
    private val steps: DynastyStepExecutor,
    private val parallelDocuments: Boolean
) : ArkistointiAdapter {
    override val canResumeAfterSuccessPersistenceFailure = true
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
            return failure(e.category.code)
        }
        catch (_: Exception) {
            return failure("DYNASTY_PAYLOAD_INVALID")
        }
        return try {
            val caseId = deliver(pyynto, plan)
            ArkistointiResult.Success(caseId.toString())
        } catch (e: DynastyFailure) {
            failure(e.category.code, e.category.retryable)
        }
    }

    private fun validateSubmission(submission: ArkistointiSubmission) {
        require(submission.execution.jobId == submission.jobId)
        require(supports(submission))
    }

    private fun deliver(submission: ArkistointiSubmission, plan: DynastyMetadataMapper.Plan): DynastyCaseId {
        val execution = submission.execution
        val caseId = steps.execute(execution, university, DynastyStepKey.Case) {
            client.createCase(plan.tosClass, plan.caseRequest)
        }
        val taskId = steps.execute(execution, university, DynastyStepKey.Task) {
            client.createTask(caseId, plan.task)
        }
        deliverDocuments(submission, plan, caseId, taskId)
        // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
        steps.execute(execution, university, DynastyStepKey.ArchiveCase) { client.archiveCase(caseId) }
        return caseId
    }

    private fun deliverDocuments(
        submission: ArkistointiSubmission, plan: DynastyMetadataMapper.Plan,
        caseId: DynastyCaseId, taskId: DynastyTaskId
    ) {
        // zip would silently drop documents if the plan and the submission ever disagreed.
        check(submission.asiakirjat.size == plan.documents.size) { "document plan does not match submission" }
        val documents = submission.asiakirjat.zip(plan.documents)
        val (mainDocument, mainMetadata) = documents.first()
        val phaseOptions = DynastyMainDocumentOptions(caseId, taskId)
        val mainId = deliverDocument(submission.execution, mainDocument, mainMetadata, phaseOptions)
        val remainingOptions = if (parallelDocuments) phaseOptions else DynastyAttachmentOptions(caseId, mainId)
        documents.drop(1).forEach { (document, metadata) ->
            deliverDocument(submission.execution, document, metadata, remainingOptions)
        }
    }

    private fun deliverDocument(
        execution: ArkistointiJobExecution, document: LoadedArkistointiAsiakirja,
        metadata: DynastyDocumentMetadata, options: DynastyDocumentOptions
    ): DynastyDocumentId = steps.execute(execution, university, DynastyStepKey.Document(document.jarjestysnumero)) {
        client.createDocument(metadata, options, document)
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
