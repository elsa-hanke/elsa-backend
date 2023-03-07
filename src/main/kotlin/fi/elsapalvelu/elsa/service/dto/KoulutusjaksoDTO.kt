package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import java.util.*
import jakarta.validation.constraints.NotNull

data class KoulutusjaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var muutOsaamistavoitteet: String? = null,

    var luotu: LocalDate? = null,

    var tallennettu: LocalDate? = null,

    @get: NotNull
    var lukittu: Boolean = false,

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var osaamistavoitteet: MutableSet<ArvioitavaKokonaisuusDTO> = mutableSetOf(),

    var koulutussuunnitelma: KoulutussuunnitelmaDTO? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutusjaksoDTO) return false
        val koulutusjaksoDTO = other
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, koulutusjaksoDTO.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}
