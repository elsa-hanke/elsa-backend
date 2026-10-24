package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.annotation.JsonFormat
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.required
import java.time.LocalDate

/** Frozen metadata for SOPIMUS and KOEJAKSO; no domain entities are needed during delivery. */
data class KoejaksoArkistointiPayload(
    val caseId: Long,
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
    val approvalDate: LocalDate?,
    val koejaksoHyvaksytty: Boolean?
) {
    override fun toString() = "KoejaksoArkistointiPayload(caseId=$caseId)"

    companion object {
        fun create(
            caseId: Long,
            studyRight: Opintooikeus,
            reviewer: String?,
            reviewDate: LocalDate?,
            approver: String?,
            approvalDate: LocalDate?,
            koejaksoHyvaksytty: Boolean? = null
        ): KoejaksoArkistointiPayload {
            val trainee = studyRight.erikoistuvaLaakari
            val user = trainee?.kayttaja?.user
            return KoejaksoArkistointiPayload(
                caseId, studyRight.id.required(), studyRight.yliopisto?.nimi.required(),
                studyRight.erikoisala?.id == YEK_ERIKOISALA_ID,
                user?.getName(), user?.firstName, user?.lastName, trainee?.syntymaaika,
                studyRight.erikoisala?.id, studyRight.erikoisala?.nimi, studyRight.opiskelijatunnus,
                reviewer, reviewDate, approver, approvalDate, koejaksoHyvaksytty
            )
        }
    }
}
