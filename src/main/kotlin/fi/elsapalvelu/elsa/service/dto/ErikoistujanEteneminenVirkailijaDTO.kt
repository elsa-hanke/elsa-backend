package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class ErikoistujanEteneminenVirkailijaDTO(

    var erikoistuvaLaakariId: Long? = null,

    var etunimi: String? = null,

    var sukunimi: String? = null,

    var syntymaaika: LocalDate? = null,

    var erikoisala: String? = null,

    var asetus: String? = null,

    var koejaksoTila: KoejaksoTila? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastotDTO? = null,

    var teoriakoulutuksetSuoritettu: Double? = null,

    var teoriakoulutuksetVaadittu: Double? = null,

    var johtamisopinnotSuoritettu: Double? = null,

    var johtamisopinnotVaadittu: Double? = null,

    var sateilysuojakoulutuksetSuoritettu: Double? = null,

    var sateilysuojakoulutuksetVaadittu: Double? = null,

    var valtakunnallisetKuulustelutSuoritettuLkm: Int? = null

): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistujanEteneminenVirkailijaDTO) return false
        if (this.erikoistuvaLaakariId == null) {
            return false
        }
        return Objects.equals(this.erikoistuvaLaakariId, other.erikoistuvaLaakariId)
    }

    override fun hashCode() = Objects.hash(this.erikoistuvaLaakariId)
}
