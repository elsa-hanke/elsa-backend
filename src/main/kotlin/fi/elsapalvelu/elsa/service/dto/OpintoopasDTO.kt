package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class OpintoopasDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloPaattyy: LocalDate? = null,

    var kaytannonKoulutuksenVahimmaispituusVuodet: Int? = null,

    var kaytannonKoulutuksenVahimmaispituusKuukaudet: Int? = null,

    var terveyskeskuskoulutusjaksonVahimmaispituusVuodet: Int? = null,

    var terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet: Int? = null,

    var terveyskeskuskoulutusjaksonMaksimipituusVuodet: Int? = null,

    var terveyskeskuskoulutusjaksonMaksimipituusKuukaudet: Int? = null,

    var yliopistosairaalajaksonVahimmaispituusVuodet: Int? = null,

    var yliopistosairaalajaksonVahimmaispituusKuukaudet: Int? = null,

    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet: Int? = null,

    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet: Int? = null,

    @get: NotNull
    var erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    var erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    var erikoisalanVaatimaJohtamisopintojenVahimmaismaara: Double? = null,

    var arviointiasteikkoId: Long? = null,

    var arviointiasteikkoNimi: ArviointiasteikkoTyyppi? = null,

    @get: NotNull
    var erikoisala: ErikoisalaDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintoopasDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
