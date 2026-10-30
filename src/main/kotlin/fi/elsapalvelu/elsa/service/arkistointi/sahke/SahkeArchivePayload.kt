package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import java.time.LocalDate

/** Approval-time values read from the immutable job payload. Never include personal data in diagnostics. */
data class SahkeArchivePayload(
    val university: String,
    val studyRightId: Long,
    val caseId: Long,
    val yek: Boolean,
    val approvalDate: LocalDate,
    val reviewer: String?,
    val reviewDate: LocalDate?,
    val approver: String?,
    val traineeName: String?,
    val firstName: String?,
    val lastName: String?,
    val birthDate: LocalDate?,
    val specialtyName: String?,
    val studentNumber: String?,
    val documents: List<ArkistointiDocumentIdentity>
) {
    override fun toString() = "SahkeArchivePayload"
}
