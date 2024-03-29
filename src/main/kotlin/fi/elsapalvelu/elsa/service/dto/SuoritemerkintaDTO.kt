package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

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

    var lisatiedot: String? = null,

    var lukittu: Boolean = false,

    var suoriteId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    var suorite: SuoriteDTO? = null,

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
