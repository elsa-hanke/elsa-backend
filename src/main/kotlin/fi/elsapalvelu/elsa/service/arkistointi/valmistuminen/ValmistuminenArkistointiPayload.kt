package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonUnwrapped
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import java.time.LocalDate

/** Approval-time metadata; the worker must not reconstruct it from mutable domain entities. */
data class ValmistuminenArkistointiPayload(
    val valmistumispyyntoId: Long,
    @get:JsonUnwrapped
    val studyRight: ArchiveStudyRightPayload,
    val reviewer: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val reviewDate: LocalDate?,
    val approver: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val approvalDate: LocalDate?,
    val documents: List<ArkistointiDocumentIdentity>,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val licensingDate: LocalDate? = null
) {
    override fun toString() = "ValmistuminenArkistointiPayload(valmistumispyyntoId=$valmistumispyyntoId)"

    companion object {
        fun create(
            request: Valmistumispyynto,
            university: YliopistoEnum,
            documents: List<ArkistointiDocumentIdentity>
        ): ValmistuminenArkistointiPayload {
            val studyRight = request.opintooikeus.required()
            return ValmistuminenArkistointiPayload(
                valmistumispyyntoId = request.id.required(),
                studyRight = ArchiveStudyRightPayload.create(studyRight, university),
                reviewer = request.virkailija?.user?.getName(),
                reviewDate = request.virkailijanKuittausaika,
                approver = request.vastuuhenkiloHyvaksyja?.user?.getName(),
                approvalDate = request.vastuuhenkiloHyvaksyjaKuittausaika,
                documents = documents.toList(),
                licensingDate = studyRight.erikoistuvaLaakari?.laillistamispaiva
            )
        }
    }
}
