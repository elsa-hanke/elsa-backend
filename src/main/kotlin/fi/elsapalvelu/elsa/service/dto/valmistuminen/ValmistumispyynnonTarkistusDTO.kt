package fi.elsapalvelu.elsa.service.dto.valmistuminen

import java.time.LocalDate
import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotKoulutustyypitDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotTilastotKoulutustyypitDTO
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

    var tyoskentelyjaksot: TyoskentelyjaksotKoulutustyypitDTO? = null,

    var terveyskeskustyoHyvaksyttyPvm: LocalDate? = null,

    var terveyskeskustyoHyvaksyntaId: Long? = null,

    var terveyskeskustyoOpintosuoritusId: Long? = null,

    var terveyskeskustyoTarkistettu: Boolean? = false,

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

    var virkailijanYhteenveto: String? = null,

    var kommentitVirkailijoille: String? = null,

    var tutkimustyotaTehty: Boolean? = null,

    var valmistumispyynto: ValmistumispyyntoDTO? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "ValmistumispyynnonTarkistusDTO"
}
