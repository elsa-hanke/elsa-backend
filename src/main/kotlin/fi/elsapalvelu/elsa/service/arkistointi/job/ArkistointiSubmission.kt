package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.util.UUID

data class ArkistointiSubmission(
    val jobId: UUID,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    val asiakirjat: List<LoadedArkistointiAsiakirja>
) {
    /** The saved job ID is the delivery key, reused for every automatic and manual attempt. */
    val key: UUID get() = jobId

    override fun toString() = "ArkistointiSubmission(" +
        "jobId=$jobId, " +
        "university=$university, " +
        "caseType=$caseType, " +
        "asiakirjoja=${asiakirjat.size})"
}
