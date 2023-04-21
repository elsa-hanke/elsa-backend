package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable
import java.time.LocalDate

data class KayttajahallintaOpintooikeusDTO(

    var id: Long? = null,

    var osaamisenArvioinninOppaanPvm: LocalDate? = null,

    var opintoopas: Long? = null

) : Serializable {
    override fun toString() = "KayttajahallintaOpintooikeusDTO"
}
