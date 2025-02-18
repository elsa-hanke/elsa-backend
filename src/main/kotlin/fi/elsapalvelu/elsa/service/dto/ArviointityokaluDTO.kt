package fi.elsapalvelu.elsa.service.dto

import jakarta.validation.constraints.NotNull
import java.io.Serializable

data class ArviointityokaluDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var ohjeteksti: String? = null,

    var kategoriaId: Long? = null,

    var kysymykset: List<ArviointityokaluKysymysDTO>? = listOf(),

) : Serializable {
    override fun toString() = "ArviointityokaluDTO"
}
