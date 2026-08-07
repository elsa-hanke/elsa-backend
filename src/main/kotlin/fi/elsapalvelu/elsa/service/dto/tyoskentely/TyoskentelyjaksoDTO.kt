package fi.elsapalvelu.elsa.service.dto.tyoskentely

import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import java.io.Serializable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
data class TyoskentelyjaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var maxAlkamispaiva: LocalDate? = null,

    var minPaattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 50)
    @get: Max(value = 100)
    var osaaikaprosentti: Int? = null,

    @get: NotNull
    var kaytannonKoulutus: KaytannonKoulutusTyyppi? = null,

    var hyvaksyttyAiempaanErikoisalaan: Boolean = false,

    @get: NotNull
    var tyoskentelypaikka: TyoskentelypaikkaDTO? = null,

    var omaaErikoisalaaTukevaId: Long? = null,

    var omaaErikoisalaaTukeva: ErikoisalaDTO? = null,

    var tapahtumia: Boolean? = null,

    var liitettyKoejaksoon: Boolean? = null,

    var asiakirjat: MutableSet<AsiakirjaDTO>? = mutableSetOf(),

    var poissaolot: List<KeskeytysaikaDTO>? = listOf(),

    var liitettyTerveyskeskuskoulutusjaksoon: Boolean? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelyjaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    companion object {
        private const val serialVersionUID = 1L
    }
}
