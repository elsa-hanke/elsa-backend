package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

data class AsiakirjaDTO (
    var id: Long? = null,

    var erikoistuvaLaakariId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var tyyppi: String? = null,

    @get: NotNull
    var lisattypvm: LocalDateTime? = null,

    @get: NotNull
    var data: ByteArray? = null

): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsiakirjaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
