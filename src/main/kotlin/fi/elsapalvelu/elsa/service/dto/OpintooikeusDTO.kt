package fi.elsapalvelu.elsa.service.dto

import com.sun.istack.NotNull
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import java.io.Serializable
import java.time.LocalDate

data class OpintooikeusDTO(

    var id: Long? = null,

    var yliopistoOpintooikeusId: String? = null,

    @get: NotNull
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @get: NotNull
    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var opiskelijatunnus: String? = null,

    @get: NotNull
    var osaamisenArvioinninOppaanPvm: LocalDate? = null,

    @get: NotNull
    var yliopistoNimi: String? = null,

    @get: NotNull
    var erikoisalaId: Long? = null,

    @get: NotNull
    var erikoisalaNimi: String? = null,

    @get: NotNull
    var opintoopasId: Long? = null,

    @get: NotNull
    var opintoopasNimi: String? = null,

    @get: NotNull
    var asetus: AsetusDTO,

    @get: NotNull
    var kaytossa: Boolean = false,

    @get: NotNull
    var tila: OpintooikeudenTila

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintooikeusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

}
