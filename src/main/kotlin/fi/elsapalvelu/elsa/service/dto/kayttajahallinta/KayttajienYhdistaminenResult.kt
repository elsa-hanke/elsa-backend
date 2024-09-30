package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import jakarta.validation.constraints.NotNull
import java.io.Serializable

data class KayttajienYhdistaminenResult(

    @get: NotNull
    var vaiheNimi: String,

    @get: NotNull
    var onnistui: Boolean = false,

    ) : Serializable {
    override fun toString() = "KayttajienYhdistaminenResult"
}

