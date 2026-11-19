package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

open class DynastyArkistointiAdapter internal constructor(
    private val destination: DynastyDestination,
    private val client: DynastyClient,
    private val metadata: DynastyMetadataMapper,
    private val steps: DynastyStepExecutor,
    properties: ApplicationProperties
) : ArkistointiAdapter {
    private val settings = destination.archiveProperties(properties.getArkistointi()).dynasty
    final override val university: YliopistoEnum = destination.university
    override val canResumeAfterSuccessPersistenceFailure = true
    override val supportedCaseTypes = destination.supportedCaseTypes

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
        check(submission.asiakirjat.size == plan.documents.size) { "document plan does not match submission" }
        val documents = submission.asiakirjat.zip(plan.documents)
        val execution = submission.execution
        val main = documents.first()
        val tosDriven = destination.documentLinking == DynastyDestination.DocumentLinking.TOS_PHASE
        val mainType = destination.profile(submission.caseType).documentTypes.getValue(main.first.asiakirjatyyppi)
        val savedCase = if (tosDriven) steps.existing(execution, settings.inProgressGrace, DynastyStepKey.Case) else null
        val savedMain = if (tosDriven) existingTosDocument(execution, main.first.jarjestysnumero) else null
        val tosPhase = if (tosDriven && savedMain == null) client.resolveTosPhase(plan.tosClass, plan.task, mainType) else null

        val caseId = savedCase ?: steps.execute(execution, settings.inProgressGrace, DynastyStepKey.Case) {
            client.createCase(plan.tosClass, plan.caseRequest)
        }
        val explicitPhase = if (tosDriven) null else steps.execute(execution, settings.inProgressGrace, DynastyStepKey.Task) {
            client.createTask(caseId, plan.task)
        }
        val phaseOptions = explicitPhase?.let { DynastyMainDocumentOptions(caseId, it) }
        val mainId = savedMain ?: deliverDocument(
            execution, main.first, main.second,
            phaseOptions ?: DynastyTosDocumentOptions(caseId, checkNotNull(tosPhase), mainType)
        )
        val remainingOptions = phaseOptions ?: DynastyAttachmentOptions(caseId, mainId)
        documents.drop(1).forEach { (document, metadata) -> deliverDocument(execution, document, metadata, remainingOptions) }
        // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
        steps.execute(execution, settings.inProgressGrace, DynastyStepKey.ArchiveCase) { client.archiveCase(caseId) }
        return caseId
    }

    private fun existingTosDocument(execution: ArkistointiJobExecution, order: Int): DynastyDocumentId? {
        // Older Turku jobs can have a TASK intent. Any unconfirmed create still needs reconciliation.
        steps.existing(execution, settings.inProgressGrace, DynastyStepKey.Task)
        return steps.existing(execution, settings.inProgressGrace, DynastyStepKey.Document(order))
    }

    private fun deliverDocument(
        execution: ArkistointiJobExecution, document: LoadedArkistointiAsiakirja,
        metadata: DynastyDocumentRequestMetadata, options: DynastyDocumentOptions
    ): DynastyDocumentId = steps.execute(execution, settings.inProgressGrace, DynastyStepKey.Document(document.jarjestysnumero)) {
        client.createDocument(metadata, options, document)
    }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
