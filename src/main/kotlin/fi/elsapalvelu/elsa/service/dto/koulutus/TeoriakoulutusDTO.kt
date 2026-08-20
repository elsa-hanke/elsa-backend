package fi.elsapalvelu.elsa.service.dto.koulutus

import java.time.LocalDate
import java.io.Serializable
import java.util.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
data class TeoriakoulutusDTO(

    var id: Long? = null,

    @get: NotNull
    var koulutuksenNimi: String? = null,

    @get: NotNull
    var koulutuksenPaikka: String? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    @get: Min(value = 0)
    var erikoistumiseenHyvaksyttavaTuntimaara: Double? = null,

    var todistukset: MutableSet<AsiakirjaDTO>? = mutableSetOf()

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TeoriakoulutusDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}
