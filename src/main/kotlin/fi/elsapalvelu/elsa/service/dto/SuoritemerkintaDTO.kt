package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class SuoritemerkintaDTO(

    var id: Long? = null,

    @get: NotNull
    var suorituspaiva: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    @Lob
    var lisatiedot: String? = null,

    var lukittu: Boolean = false,

    var oppimistavoiteId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    var oppimistavoite: OppimistavoiteDTO? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritemerkintaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
