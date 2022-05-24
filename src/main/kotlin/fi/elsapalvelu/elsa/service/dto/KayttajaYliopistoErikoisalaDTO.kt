package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

data class KayttajaYliopistoErikoisalaDTO   (

    var id: Long? = null,

    var kayttajaId: Long? = null,

    @get: NotNull
    var yliopisto: YliopistoDTO? = null,

    @get: NotNull
    var erikoisala: ErikoisalaDTO? = null,

    var vastuuhenkilonTehtavat: MutableSet<VastuuhenkilonTehtavatyyppiDTO> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaYliopistoErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaYliopistoErikoisalaDTO{id=$id}"
}
