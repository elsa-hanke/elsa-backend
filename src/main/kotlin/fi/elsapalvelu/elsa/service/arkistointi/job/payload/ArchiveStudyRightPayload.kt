package fi.elsapalvelu.elsa.service.arkistointi.job.payload

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.required
import java.time.LocalDate

/** Captured during approval; delivery reads these saved values through ArchivePayloadReader. */
data class ArchiveStudyRightPayload(
    @get:JsonProperty(ArchivePayloadFields.STUDY_RIGHT_ID)
    val opintooikeusId: Long,
    @get:JsonProperty(ArchivePayloadFields.UNIVERSITY)
    val university: YliopistoEnum,
    @get:JsonProperty(ArchivePayloadFields.YEK)
    val yek: Boolean,
    @get:JsonProperty(ArchivePayloadFields.TRAINEE_NAME)
    val traineeName: String?,
    @get:JsonProperty(ArchivePayloadFields.FIRST_NAME)
    val firstName: String?,
    @get:JsonProperty(ArchivePayloadFields.LAST_NAME)
    val lastName: String?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    @get:JsonProperty(ArchivePayloadFields.BIRTH_DATE)
    val birthDate: LocalDate?,
    @get:JsonProperty(ArchivePayloadFields.SPECIALTY_ID)
    val specialtyId: Long?,
    @get:JsonProperty(ArchivePayloadFields.SPECIALTY_NAME)
    val specialtyName: String?,
    @get:JsonProperty(ArchivePayloadFields.STUDENT_NUMBER)
    val studentNumber: String?,
    @get:JsonProperty(ArchivePayloadFields.SPECIALTY_TYPE)
    val specialtyType: ErikoisalaTyyppi?,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    @get:JsonProperty(ArchivePayloadFields.STUDY_RIGHT_START_DATE)
    val studyRightStartDate: LocalDate?,
    @get:JsonProperty(ArchivePayloadFields.DEGREE_REGULATION)
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
