package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class KoulutussuunnitelmaDTO(

    var id: Long? = null,

    var motivaatiokirje: String? = null,

    @get: NotNull
    var motivaatiokirjeYksityinen: Boolean? = null,

    var opiskeluJaTyohistoria: String? = null,

    @get: NotNull
    var opiskeluJaTyohistoriaYksityinen: Boolean? = null,

    var vahvuudet: String? = null,

    @get: NotNull
    var vahvuudetYksityinen: Boolean? = null,

    var tulevaisuudenVisiointi: String? = null,

    @get: NotNull
    var tulevaisuudenVisiointiYksityinen: Boolean? = null,

    var osaamisenKartuttaminen: String? = null,

    @get: NotNull
    var osaamisenKartuttaminenYksityinen: Boolean? = null,

    var elamankentta: String? = null,

    @get: NotNull
    var elamankenttaYksityinen: Boolean? = null,

    var koulutussuunnitelmaAsiakirja: AsiakirjaDTO? = null,

    var koulutussuunnitelmaAsiakirjaUpdated: Boolean = false,

    var motivaatiokirjeAsiakirja: AsiakirjaDTO? = null,

    var motivaatiokirjeAsiakirjaUpdated: Boolean = false,

    var muokkauspaiva: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutussuunnitelmaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
