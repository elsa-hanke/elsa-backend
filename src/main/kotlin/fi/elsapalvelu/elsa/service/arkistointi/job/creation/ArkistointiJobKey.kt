package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

/** Persisted business-key namespaces; changing these prefixes would bypass existing-job checks. */
object ArkistointiJobKey {
    fun create(university: YliopistoEnum, caseType: CaseType, caseId: Long): String {
        val prefix = when (caseType) {
            CaseType.VALMISTUMINEN -> "valmistuminen"
            CaseType.KOEJAKSO -> "koejakso-arvio"
            CaseType.SOPIMUS -> "koejakso-sopimus"
        }
        return "$prefix:${university.name}:$caseId"
    }
}
