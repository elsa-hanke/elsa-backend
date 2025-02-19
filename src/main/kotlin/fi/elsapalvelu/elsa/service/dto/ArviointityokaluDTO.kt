package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.ArviointityokaluKategoria
import jakarta.validation.constraints.NotNull
import java.io.Serializable

data class ArviointityokaluDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var ohjeteksti: String? = null,

    var kategoria: ArviointityokaluKategoria? = null,

    var kysymykset: List<ArviointityokaluKysymysDTO>? = listOf(),

    ) : Serializable {
    override fun toString() = "ArviointityokaluDTO"
}
