package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class ValmistumispyynnonTarkistusDTO(

    var id: Long? = null,

    var yekSuoritettu: Boolean? = null,

    var yekSuorituspaiva: LocalDate? = null,

    var ptlSuoritettu: Boolean? = null,

    var ptlSuorituspaiva: LocalDate? = null,

    var aiempiElKoulutusSuoritettu: Boolean? = null,

    var aiempiElKoulutusSuorituspaiva: LocalDate? = null,

    var ltTutkintoSuoritettu: Boolean? = null,

    var ltTutkintoSuorituspaiva: LocalDate? = null,

    var yliopistosairaalanUlkopuolinenTyoTarkistettu: Boolean? = null,

    var yliopistosairaalatyoTarkistettu: Boolean? = null,

    var kokonaistyoaikaTarkistettu: Boolean? = null,

    var teoriakoulutusTarkistettu: Boolean? = null,

    var kommentitVirkailijoille: String? = null,

    var keskenerainen: Boolean? = null,

    var valmistumispyynto: ValmistumispyyntoDTO? = null

) : Serializable {
    override fun toString() = "ValmistumispyynnonTarkistusDTO"
}
