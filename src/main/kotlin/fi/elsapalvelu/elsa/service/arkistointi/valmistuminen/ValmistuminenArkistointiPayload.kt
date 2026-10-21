package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.required
import java.time.LocalDate
import com.fasterxml.jackson.annotation.JsonFormat

/** Approval-time metadata; the worker must not reconstruct it from mutable domain entities. */
data class ValmistuminenArkistointiPayload(
    val valmistumispyyntoId: Long,
    val opintooikeusId: Long,
    val university: YliopistoEnum,
    val yek: Boolean,
    val traineeName: String?,
    val firstName: String?,
    val lastName: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val birthDate: LocalDate?,
    val specialtyId: Long?,
    val specialtyName: String?,
    val studentNumber: String?,
    val reviewer: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val reviewDate: LocalDate?,
    val approver: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val approvalDate: LocalDate?
) {
    override fun toString() = "ValmistuminenArkistointiPayload(valmistumispyyntoId=$valmistumispyyntoId)"

    companion object {
        fun create(request: Valmistumispyynto, university: YliopistoEnum): ValmistuminenArkistointiPayload {
            val studyRight = request.opintooikeus.required()
            val trainee = studyRight.erikoistuvaLaakari
            val user = trainee?.kayttaja?.user
            return ValmistuminenArkistointiPayload(
                valmistumispyyntoId = request.id.required(),
                opintooikeusId = studyRight.id.required(),
                university = university,
                yek = request.isYek(),
                traineeName = user?.getName(),
                firstName = user?.firstName,
                lastName = user?.lastName,
                birthDate = trainee?.syntymaaika,
                specialtyId = studyRight.erikoisala?.id,
                specialtyName = studyRight.erikoisala?.nimi,
                studentNumber = studyRight.opiskelijatunnus,
                reviewer = request.virkailija?.user?.getName(),
                reviewDate = request.virkailijanKuittausaika,
                approver = request.vastuuhenkiloHyvaksyja?.user?.getName(),
                approvalDate = request.vastuuhenkiloHyvaksyjaKuittausaika
            )
        }
    }
}
