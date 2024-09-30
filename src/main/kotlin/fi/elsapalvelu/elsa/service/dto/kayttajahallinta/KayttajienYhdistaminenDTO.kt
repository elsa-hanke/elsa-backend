package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import jakarta.validation.constraints.NotNull
import java.io.Serializable

data class KayttajienYhdistaminenDTO(

    @get: NotNull
    var ensimmainenKayttajaId: Long? = null,

    @get: NotNull
    var toinenKayttajaId: Long? = null,

    @get: NotNull
    var yhteinenSahkoposti: String,

    ) : Serializable {
    override fun toString() = "KayttajienYhdistaminenDTO"
}

