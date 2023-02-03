package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class SuoritusarvioinninArvioitavaKokonaisuusDTO(

    var id: Long? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiArviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    @get: NotNull
    var arvioitavaKokonaisuusId: Long? = null,

    var arvioitavaKokonaisuus: ArvioitavaKokonaisuusDTO? = null,

    var suoritusarviointiId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarvioinninArvioitavaKokonaisuusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
