package fi.elsapalvelu.elsa.service.arkistointi.job.payload

import com.fasterxml.jackson.annotation.JsonFormat
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.required
import java.time.LocalDate

/** Captured during approval; delivery reads these saved values through ArchivePayloadReader. */
data class ArchiveStudyRightPayload(
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
    val specialtyType: ErikoisalaTyyppi?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val studyRightStartDate: LocalDate?,
    val degreeRegulation: String?
) {
    override fun toString() = "ArchiveStudyRightPayload"

    companion object {
        fun create(studyRight: Opintooikeus, university: YliopistoEnum): ArchiveStudyRightPayload {
            val trainee = studyRight.erikoistuvaLaakari
            val user = trainee?.kayttaja?.user
            return ArchiveStudyRightPayload(
                opintooikeusId = studyRight.id.required(),
                university = university,
                yek = studyRight.isYek(),
                traineeName = user?.getName(),
                firstName = user?.firstName,
                lastName = user?.lastName,
                birthDate = trainee?.syntymaaika,
                specialtyId = studyRight.erikoisala?.id,
                specialtyName = studyRight.erikoisala?.nimi,
                studentNumber = studyRight.opiskelijatunnus,
                specialtyType = studyRight.erikoisala?.tyyppi,
                studyRightStartDate = studyRight.opintooikeudenMyontamispaiva,
                degreeRegulation = studyRight.asetus?.nimi
            )
        }
    }
}
