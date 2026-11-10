package fi.elsapalvelu.elsa.service.arkistointi.job.payload

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import java.time.LocalDate

/** Shared writer contract. JSON names stay stable when Kotlin properties are renamed. */
interface ArchiveApprovalPayload {
    @get:JsonProperty(ArchivePayloadFields.VERSION)
    val version: Int get() = ArchivePayloadFields.CURRENT_VERSION

    @get:JsonProperty(ArchivePayloadFields.CASE_ID)
    val caseId: Long

    @get:JsonUnwrapped
    val studyRight: ArchiveStudyRightPayload

    @get:JsonProperty(ArchivePayloadFields.REVIEWER)
    val reviewer: String?

    @get:JsonProperty(ArchivePayloadFields.REVIEW_DATE)
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val reviewDate: LocalDate?

    @get:JsonProperty(ArchivePayloadFields.APPROVER)
    val approver: String?

    @get:JsonProperty(ArchivePayloadFields.APPROVAL_DATE)
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val approvalDate: LocalDate?

    @get:JsonProperty(ArchivePayloadFields.DOCUMENTS)
    val documents: List<ArkistointiDocumentIdentity>
}

/** Persisted wire names, shared by Jackson writers and the strict reader. */
internal object ArchivePayloadFields {
    const val CURRENT_VERSION = 1
    const val VERSION = "version"
    const val CASE_ID = "caseId"
    const val STUDY_RIGHT_ID = "opintooikeusId"
    const val UNIVERSITY = "university"
    const val YEK = "yek"
    const val REVIEWER = "reviewer"
    const val REVIEW_DATE = "reviewDate"
    const val APPROVER = "approver"
    const val APPROVAL_DATE = "approvalDate"
    const val TRAINEE_NAME = "traineeName"
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val BIRTH_DATE = "birthDate"
    const val SPECIALTY_ID = "specialtyId"
    const val SPECIALTY_NAME = "specialtyName"
    const val STUDENT_NUMBER = "studentNumber"
    const val SPECIALTY_TYPE = "specialtyType"
    const val STUDY_RIGHT_START_DATE = "studyRightStartDate"
    const val DEGREE_REGULATION = "degreeRegulation"
    const val LICENSING_DATE = "licensingDate"
    const val ASSESSMENT_ACCEPTED = "koejaksoHyvaksytty"
    const val DOCUMENTS = "documents"
}
