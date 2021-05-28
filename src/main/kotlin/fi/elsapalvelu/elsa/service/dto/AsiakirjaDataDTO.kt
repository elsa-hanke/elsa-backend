package fi.elsapalvelu.elsa.service.dto

import java.io.InputStream
import java.io.Serializable
import javax.validation.constraints.NotNull

data class AsiakirjaDataDTO(

    var id: Long? = null,

    @get: NotNull
    var fileInputStream: InputStream? = null,

    @get: NotNull
    var fileSize: Long? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsiakirjaDataDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}


