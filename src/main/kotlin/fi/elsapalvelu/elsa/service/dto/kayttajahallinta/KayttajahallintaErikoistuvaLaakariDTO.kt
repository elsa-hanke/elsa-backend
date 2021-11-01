package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KayttajahallintaErikoistuvaLaakariDTO(

    @get: NotNull
    var etunimi: String? = null,

    @get: NotNull
    var sukunimi: String? = null,

    @get: NotNull
    var yliopistoId: Long? = null,

    @get: NotNull
    var erikoisalaId: Long? = null,

    @get: NotNull
    var opiskelijatunnus: String? = null,

    @get: NotNull
    var opiskeluoikeusAlkaa: LocalDate? = null,

    @get: NotNull
    var opiskeluoikeusPaattyy: LocalDate? = null,

    @get: NotNull
    var opintosuunnitelmaKaytossaPvm: LocalDate? = null,

    @get: NotNull
    var sahkopostiosoite: String? = null

) : Serializable {
    override fun toString() = "ErikoistuvaLaakariDTO"
}

