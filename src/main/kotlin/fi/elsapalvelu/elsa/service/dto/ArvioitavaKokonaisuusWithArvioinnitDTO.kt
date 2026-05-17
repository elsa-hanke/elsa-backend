package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiByKokonaisuusDTO

data class ArvioitavaKokonaisuusWithArvioinnitDTO : Serializable {
    var id: Long? = null
    @get: NotNull
    var nimi: String? = null
    var nimiSv: String? = null
    var kuvaus: String? = null
    var kuvausSv: String? = null
    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null
    var voimassaoloLoppuu: LocalDate? = null
    var suoritusarvioinnit: List<SuoritusarviointiByKokonaisuusDTO> = listOf()

    companion object {
        private const val serialVersionUID = 1L
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusWithArvioinnitDTO) return false
        return id != null && id == other.id
    }
    override fun hashCode() = 31
}
