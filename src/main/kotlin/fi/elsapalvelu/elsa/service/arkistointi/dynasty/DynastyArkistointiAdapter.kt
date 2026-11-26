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
    private val archiveProfile: DynastyArchiveProfile,
    private val client: DynastyClient,
    private val metadata: DynastyMetadataMapper,
    private val steps: DynastyStepExecutor,
    properties: ApplicationProperties
) : ArkistointiAdapter {
    private val settings = archiveProfile.archiveProperties(properties.getArkistointi()).dynasty
    final override val university: YliopistoEnum = archiveProfile.university
    override val canResumeAfterSuccessPersistenceFailure = true
    override val supportedCaseTypes = archiveProfile.supportedCaseTypes

    override fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        client.validateConfiguration()
        metadata.validateConfiguration()
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
        val caseId = deliverToTosPhase(submission, plan, documents)
        // Conservatively park an ambiguous state change too, until vendor confirms replay semantics.
        steps.execute(execution, settings.inProgressGrace, DynastyStepKey.ArchiveCase) { client.archiveCase(caseId) }
        return caseId
    }

    private fun deliverToTosPhase(
        submission: ArkistointiSubmission, plan: DynastyMetadataMapper.Plan,
        documents: List<Pair<LoadedArkistointiAsiakirja, DynastyTosDocumentMetadata>>
    ): DynastyCaseId {
        val execution = submission.execution
        val savedCase = steps.existing(execution, settings.inProgressGrace, DynastyStepKey.Case)
        val pendingDocuments = documents.filter { (document, _) ->
            steps.existing(execution, settings.inProgressGrace, DynastyStepKey.Document(document.jarjestysnumero)) == null
        }
        if (pendingDocuments.isEmpty()) return checkNotNull(savedCase)

        val tosTitlesByRecordType = archiveProfile.caseProfile(submission.caseType).documentTypes
        val tosTitles = pendingDocuments.map { (document, _) -> tosTitlesByRecordType.getValue(document.asiakirjatyyppi) }.toSet()
        val phaseId = client.resolveTosPhase(plan.tosClass, plan.task, tosTitles)
        val caseId = savedCase ?: createCase(execution, plan)
        pendingDocuments.forEach { (document, metadata) ->
            val options = DynastyTosPhaseOptions(caseId, phaseId, tosTitlesByRecordType.getValue(document.asiakirjatyyppi))
            steps.execute(execution, settings.inProgressGrace, DynastyStepKey.Document(document.jarjestysnumero)) {
                client.createDocument(metadata, options, document, pascalCaseMultipartNames = archiveProfile == DynastyArchiveProfile.UEF)
            }
        }
        return caseId
    }

    private fun createCase(execution: ArkistointiJobExecution, plan: DynastyMetadataMapper.Plan): DynastyCaseId =
        steps.execute(execution, settings.inProgressGrace, DynastyStepKey.Case) { client.createCase(plan.tosClass, plan.caseRequest) }

    private fun failure(code: String, retryable: Boolean = false): ArkistointiResult =
        ArkistointiResult.failure(code, "Dynasty-arkistoinnin tila tai asetukset on tarkistettava", retryable)
}
