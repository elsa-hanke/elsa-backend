package fi.elsapalvelu.elsa.service.arkistointi.support

import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.util.UUID

/** Explicit execution for adapter/package fixtures; processor tests supply their actual claimed execution. */
fun arkistointiSubmission(
    jobId: UUID,
    university: YliopistoEnum,
    caseType: CaseType,
    payload: String,
    asiakirjat: List<LoadedArkistointiAsiakirja>,
    version: Long = 1
): ArkistointiSubmission = ArkistointiSubmission(
    jobId, university, caseType, payload, asiakirjat,
    ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), version)
)
