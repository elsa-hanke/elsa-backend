package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class KayttajahallintaErikoistuvaLaakariDTO(

    @get: NotEmpty
    var etunimi: String? = null,

    @get: NotEmpty
    var sukunimi: String? = null,

    @get: NotEmpty
    @get: Email
    var sahkopostiosoite: String? = null,

    @get: NotNull
    var yliopistoId: Long? = null,

    @get: NotNull
    var erikoisalaId: Long? = null,

    var opiskelijatunnus: String? = null,

    @get: NotNull
    var opintooikeusAlkaa: LocalDate? = null,

    @get: NotNull
    var opintooikeusPaattyy: LocalDate? = null,

    @get: NotNull
    var asetusId: Long? = null,

    @get: NotNull
    var opintoopasId: Long? = null,

    @get: NotNull
    var osaamisenArvioinninOppaanPvm: LocalDate? = null

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariDTO"
}

