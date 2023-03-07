package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

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
}
