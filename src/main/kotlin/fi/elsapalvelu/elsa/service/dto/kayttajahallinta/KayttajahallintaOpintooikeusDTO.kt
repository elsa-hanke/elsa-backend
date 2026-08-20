package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.time.LocalDate
import java.io.Serializable

data class KayttajahallintaOpintooikeusDTO(

    var id: Long? = null,

    var osaamisenArvioinninOppaanPvm: LocalDate? = null,

    var opintoopas: Long? = null

) : Serializable {
    override fun toString() = "KayttajahallintaOpintooikeusDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}
