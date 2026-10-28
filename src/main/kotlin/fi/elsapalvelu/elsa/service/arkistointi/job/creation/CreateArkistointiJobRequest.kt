package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

data class CreateArkistointiJobRequest(
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    /** Local business key for finding the existing job, not the external delivery identity. */
    val key: String,
    val asiakirjat: List<ArkistointiAsiakirjaReference>
) {
    override fun toString() = "CreateArkistointiJobRequest(" +
        "university=$university, " +
        "caseType=$caseType, " +
        "key=$key, " +
        "asiakirjoja=${asiakirjat.size})"
}
