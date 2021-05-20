package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.service.projection.AsiakirjaListProjection
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class TyoskentelyjaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

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

    var erikoistuvaLaakariId: Long? = null,

    var omaaErikoisalaaTukeva: ErikoisalaDTO? = null,

    var suoritusarvioinnit: Boolean? = null,

    var liitettyKoejaksoon: Boolean? = null,

    var asiakirjat: MutableList<AsiakirjaListProjection>? = null,

    var kaikkiAsiakirjaNimet: MutableSet<String>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelyjaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
