package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveApprovalPayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadFields
import java.time.LocalDate

/** Approval-time metadata; the worker must not reconstruct it from mutable domain entities. */
data class ValmistuminenArkistointiPayload(
    override val caseId: Long,
    override val studyRight: ArchiveStudyRightPayload,
    override val reviewer: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    override val reviewDate: LocalDate?,
    override val approver: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    override val approvalDate: LocalDate?,
    override val documents: List<ArkistointiDocumentIdentity>,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    @get:JsonProperty(ArchivePayloadFields.LICENSING_DATE)
    val licensingDate: LocalDate? = null
) : ArchiveApprovalPayload {
    override fun toString() = "ValmistuminenArkistointiPayload(caseId=$caseId)"

    companion object {
        fun create(
            valmistumispyynto: Valmistumispyynto,
            university: YliopistoEnum,
            documents: List<ArkistointiDocumentIdentity>
        ): ValmistuminenArkistointiPayload {
            val opintooikeus = valmistumispyynto.opintooikeus.required()
            return ValmistuminenArkistointiPayload(
                caseId = valmistumispyynto.id.required(),
                studyRight = ArchiveStudyRightPayload.create(opintooikeus, university),
                reviewer = valmistumispyynto.virkailija?.user?.getName(),
                reviewDate = valmistumispyynto.virkailijanKuittausaika,
                approver = valmistumispyynto.vastuuhenkiloHyvaksyja?.user?.getName(),
                approvalDate = valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika,
                documents = documents.toList(),
                licensingDate = opintooikeus.erikoistuvaLaakari?.laillistamispaiva
            )
        }
    }
}
