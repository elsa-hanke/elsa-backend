package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class ErikoisalaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloPaattyy: LocalDate? = null,

    @get: NotNull
    var tyyppi: ErikoisalaTyyppi? = null,

    @get: NotNull
    var kaytannonKoulutuksenVahimmaispituus: Double? = null,

    @get: NotNull
    var terveyskeskuskoulutusjaksonVahimmaispituus: Double? = null,

    @get: NotNull
    var yliopistosairaalajaksonVahimmaispituus: Double? = null,

    @get: NotNull
    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus: Double? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
