package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class KoulutettavanEteneminenDTO(

    var opintooikeusId: Long? = null,

    var etunimi: String? = null,

    var sukunimi: String? = null,

    var syntymaaika: LocalDate? = null,

    var erikoisala: String? = null,

    var asetus: String? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastotDTO? = null,

    var teoriakoulutuksetSuoritettu: Boolean? = null,

    var yekSuoritettu: Boolean? = null

): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutettavanEteneminenDTO) return false
        if (this.opintooikeusId == null) {
            return false
        }
        return Objects.equals(this.opintooikeusId, other.opintooikeusId)
    }

    override fun hashCode() = Objects.hash(this.opintooikeusId)
}
