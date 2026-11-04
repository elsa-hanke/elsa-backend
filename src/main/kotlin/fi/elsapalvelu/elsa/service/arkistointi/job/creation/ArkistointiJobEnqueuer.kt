package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveApprovalPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Approval callers hold the case row lock; PDF creation, snapshots and scheduling join their transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class ArkistointiJobEnqueuer(
    private val jobs: ArkistointiJobRepository,
    private val snapshots: ArkistointiAsiakirjaSnapshotService,
    private val creator: ArkistointiJobCreator,
    private val mapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enqueue(
        university: YliopistoEnum,
        caseType: CaseType,
        caseId: Long,
        records: () -> List<RecordProperties>,
        createPayload: (List<ArkistointiDocumentIdentity>) -> ArchiveApprovalPayload
    ) {
        val key = ArkistointiJobKey.create(university, caseType, caseId)
        // Do not evaluate records for an existing job: it may generate PDFs and prepare notifications.
        if (jobs.findByKey(key) != null) {
            log.info("Arkistointi-job on jo olemassa [key={}]", key)
            return
        }
        val identities = mutableListOf<ArkistointiDocumentIdentity>()
        val references = records().mapIndexed { index, record ->
            val sourceId = record.asiakirja.id.required()
            snapshots.createReference(record.asiakirja, record.type, index).also { reference ->
                identities.add(reference.identity(sourceId))
            }
        }
        creator.create(CreateArkistointiJobRequest(
            university = university, caseType = caseType, key = key,
            payload = mapper.writeValueAsString(createPayload(identities.toList())), asiakirjat = references
        ))
        log.info("Arkistointi-job luotu [key={}]", key)
    }
}
