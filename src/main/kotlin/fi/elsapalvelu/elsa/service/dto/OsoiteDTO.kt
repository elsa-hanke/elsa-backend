package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.Osoite] entity.
 */
data class OsoiteDTO(

    var id: Long? = null,

    @get: NotNull
    var ensisijainen: Boolean? = null,

    @get: NotNull
    var osoiterivi1: String? = null,

    var osoiterivi2: String? = null,

    var osoiterivi3: String? = null,

    @get: NotNull
    var kunta: String? = null,

    @get: NotNull
    var postinumero: Int? = null,

    var maakunta: String? = null,

    @get: NotNull
    var maa: String? = null,

    var erikoistuvaLaakariId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsoiteDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
