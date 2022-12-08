package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class UusiValmistumispyyntoDTO(

    var selvitysVanhentuneistaSuorituksista: String? = null,

    var laillistamispaiva: LocalDate? = null,

    var erikoistujanPuhelinnumero: String? = null,

    var erikoistujanSahkoposti: String? = null

) : Serializable {
    override fun toString() = "UusiValmistumispyyntoDTO"
}
