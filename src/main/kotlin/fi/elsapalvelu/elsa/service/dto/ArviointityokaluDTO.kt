package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.ArviointityokaluKategoria
import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.domain.enumeration.ArviointityokalunTila
import jakarta.validation.constraints.NotNull
import java.io.Serializable

data class ArviointityokaluDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var ohjeteksti: String? = null,

    var kategoria: ArviointityokaluKategoria? = null,

    var kysymykset: List<ArviointityokaluKysymysDTO>? = listOf(),

    var tila: ArviointityokalunTila? = null,

    var liite: AsiakirjaData? = null,

    var liitetiedostonNimi: String? = null,

    var liitetiedostonTyyppi: String? = null,

    ) : Serializable {
    override fun toString() = "ArviointityokaluDTO"
}
