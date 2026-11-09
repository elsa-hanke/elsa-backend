package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveApprovalPayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadFields
import java.time.LocalDate

/** Frozen metadata for SOPIMUS and KOEJAKSO; no domain entities are needed during delivery. */
data class KoejaksoArkistointiPayload(
    override val caseId: Long,
    override val studyRight: ArchiveStudyRightPayload,
    override val reviewer: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    override val reviewDate: LocalDate?,
    override val approver: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    override val approvalDate: LocalDate?,
    @get:JsonProperty(ArchivePayloadFields.ASSESSMENT_ACCEPTED)
    val koejaksoHyvaksytty: Boolean?,
    override val documents: List<ArkistointiDocumentIdentity>
) : ArchiveApprovalPayload {
    override fun toString() = "KoejaksoArkistointiPayload(caseId=$caseId)"

    companion object {
        fun create(
            caseId: Long,
            studyRight: Opintooikeus,
            documents: List<ArkistointiDocumentIdentity>,
            reviewer: String?,
            reviewDate: LocalDate?,
            approver: String?,
            approvalDate: LocalDate?,
            koejaksoHyvaksytty: Boolean? = null
        ): KoejaksoArkistointiPayload {
            return KoejaksoArkistointiPayload(
                caseId = caseId,
                studyRight = ArchiveStudyRightPayload.create(studyRight, studyRight.yliopisto?.nimi.required()),
                reviewer = reviewer,
                reviewDate = reviewDate,
                approver = approver,
                approvalDate = approvalDate,
                koejaksoHyvaksytty = koejaksoHyvaksytty,
                documents = documents.toList()
            )
        }
    }
}
