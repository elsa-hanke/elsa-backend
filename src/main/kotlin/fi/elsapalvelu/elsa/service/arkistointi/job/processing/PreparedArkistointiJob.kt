package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentDescriptor
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.util.UUID

data class PreparedArkistointiJob(
    val jobId: UUID,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    val asiakirjat: List<ArkistointiDocumentDescriptor>
) {
    override fun toString() = "PreparedArkistointiJob(jobId=$jobId, asiakirjoja=${asiakirjat.size})"
}
