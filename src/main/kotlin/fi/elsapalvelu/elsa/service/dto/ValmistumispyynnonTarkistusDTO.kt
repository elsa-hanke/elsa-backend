package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class ValmistumispyynnonTarkistusDTO(

    var id: Long? = null,

    var yekSuoritettu: Boolean? = false,

    var yekSuorituspaiva: LocalDate? = null,

    var ptlSuoritettu: Boolean? = false,

    var ptlSuorituspaiva: LocalDate? = null,

    var aiempiElKoulutusSuoritettu: Boolean? = false,

    var aiempiElKoulutusSuorituspaiva: LocalDate? = null,

    var ltTutkintoSuoritettu: Boolean? = false,

    var ltTutkintoSuorituspaiva: LocalDate? = null,

    var yliopistosairaalanUlkopuolinenTyoTarkistettu: Boolean? = false,

    var yliopistosairaalatyoTarkistettu: Boolean? = false,

    var kokonaistyoaikaTarkistettu: Boolean? = false,

    var teoriakoulutusTarkistettu: Boolean? = false,

    var kommentitVirkailijoille: String? = null,

    var keskenerainen: Boolean? = false,

    var valmistumispyynto: ValmistumispyyntoDTO? = null

) : Serializable {
    override fun toString() = "ValmistumispyynnonTarkistusDTO"
}
