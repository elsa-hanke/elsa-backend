package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KoulutussuunnitelmaDTO(

    var id: Long? = null,

    @Lob var motivaatiokirje: String? = null,

    @get: NotNull
    var motivaatiokirjeYksityinen: Boolean? = null,

    @Lob var opiskeluJaTyohistoria: String? = null,

    @get: NotNull
    var opiskeluJaTyohistoriaYksityinen: Boolean? = null,

    @Lob var vahvuudet: String? = null,

    @get: NotNull
    var vahvuudetYksityinen: Boolean? = null,

    @Lob var tulevaisuudenVisiointi: String? = null,

    @get: NotNull
    var tulevaisuudenVisiointiYksityinen: Boolean? = null,

    @Lob var osaamisenKartuttaminen: String? = null,

    @get: NotNull
    var osaamisenKartuttaminenYksityinen: Boolean? = null,

    @Lob var elamankentta: String? = null,

    @get: NotNull
    var elamankenttaYksityinen: Boolean? = null,

    @get: NotNull
    var erikoistuvaLaakariId: Long? = null,

    var koulutussuunnitelmaAsiakirja: AsiakirjaDTO? = null,

    var koulutussuunnitelmaAsiakirjaUpdated: Boolean = false,

    var motivaatiokirjeAsiakirja: AsiakirjaDTO? = null,
    
    var motivaatiokirjeAsiakirjaUpdated: Boolean = false
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutussuunnitelmaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
