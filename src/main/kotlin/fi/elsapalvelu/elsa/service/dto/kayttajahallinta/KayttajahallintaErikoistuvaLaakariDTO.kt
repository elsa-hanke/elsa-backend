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
    var opintooikeusAlkaa: LocalDate? = null,

    @get: NotNull
    var opintooikeusPaattyy: LocalDate? = null,

    @get: NotNull
    var opintosuunnitelmaKaytossaPvm: LocalDate? = null,

    @get: NotNull
    var sahkopostiosoite: String? = null

) : Serializable {
    override fun toString() = "ErikoistuvaLaakariDTO"
}

