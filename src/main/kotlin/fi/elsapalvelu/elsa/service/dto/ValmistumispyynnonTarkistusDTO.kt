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

    var tyoskentelyjaksotTilastot: TyoskentelyjaksotTilastotKoulutustyypitDTO? = null,

    var terveyskeskustyoHyvaksyttyPvm: LocalDate? = null,

    var terveyskeskustyoHyvaksyntaId: Long? = null,

    var terveyskeskustyoOpintosuoritusId: Long? = null,

    var yliopistosairaalanUlkopuolinenTyoTarkistettu: Boolean? = false,

    var yliopistosairaalatyoTarkistettu: Boolean? = false,

    var kokonaistyoaikaTarkistettu: Boolean? = false,

    var teoriakoulutusSuoritettu: Double? = null,

    var teoriakoulutusVaadittu: Double? = null,

    var teoriakoulutusTarkistettu: Boolean? = false,

    var sateilusuojakoulutusSuoritettu: Double? = null,

    var sateilusuojakoulutusVaadittu: Double? = null,

    var johtamiskoulutusSuoritettu: Double? = null,

    var johtamiskoulutusVaadittu: Double? = null,

    var kuulustelut: List<OpintosuoritusDTO>? = listOf(),

    var koejaksoHyvaksyttyPvm: LocalDate? = null,

    var koejaksoEiVaadittu: Boolean? = false,

    var suoritustenTila: ValmistumispyyntoSuoritustenTilaDTO? = null,

    var kommentitVirkailijoille: String? = null,

    var valmistumispyynto: ValmistumispyyntoDTO? = null

) : Serializable {
    override fun toString() = "ValmistumispyynnonTarkistusDTO"
}
