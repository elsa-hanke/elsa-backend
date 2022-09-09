package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class TerveyskeskuskoulutusjaksoUpdateDTO(

    var korjausehdotus: String? = null,

    var lisatiedotVirkailijalta: String? = null

) : Serializable {
    override fun toString() = "TerveyskeskuskoulutusjaksoUpdateDTO"
}
