package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class OpintoopasDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloPaattyy: LocalDate? = null,

    @get: NotNull
    var kaytannonKoulutuksenVahimmaispituus: Double? = null,

    @get: NotNull
    var terveyskeskuskoulutusjaksonVahimmaispituus: Double? = null,

    var terveyskeskuskoulutusjaksonMaksimipituus: Double? = null,

    @get: NotNull
    var yliopistosairaalajaksonVahimmaispituus: Double? = null,

    @get: NotNull
    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus: Double? = null,

    @get: NotNull
    var erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    var erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    var erikoisalanVaatimaJohtamisopintojenVahimmaismaara: Double? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintoopasDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
