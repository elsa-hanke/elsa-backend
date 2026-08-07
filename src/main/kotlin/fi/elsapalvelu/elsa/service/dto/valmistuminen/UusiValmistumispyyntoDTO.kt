package fi.elsapalvelu.elsa.service.dto.valmistuminen

import java.time.LocalDate
import java.io.Serializable

data class UusiValmistumispyyntoDTO(

    var selvitysVanhentuneistaSuorituksista: String? = null,

    var laillistamispaiva: LocalDate? = null,

    var erikoistujanPuhelinnumero: String? = null,

    var erikoistujanSahkoposti: String? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "UusiValmistumispyyntoDTO"
}
