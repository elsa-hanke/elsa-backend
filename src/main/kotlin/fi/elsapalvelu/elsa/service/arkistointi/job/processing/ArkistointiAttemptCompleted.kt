package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.util.UUID

data class ArkistointiAttemptCompleted(
    val jobId: UUID,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val attempt: Int,
    val outcome: Outcome,
    val errorCode: String? = null
) {
    enum class Outcome { SUCCESS, RETRYABLE_FAILURE, TERMINAL_FAILURE }
}
