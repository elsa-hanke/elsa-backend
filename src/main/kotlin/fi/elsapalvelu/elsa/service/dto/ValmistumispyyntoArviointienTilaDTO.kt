package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoArviointienTilaDTO (

    var hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour: Boolean = false,

    var hasArvioitaviaKokonaisuuksiaWithoutArviointi: Boolean = false

) : Serializable {
    override fun toString() = "ValmistumispyyntoArviointienTilaDTO"
}
