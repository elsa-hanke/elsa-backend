package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable
import java.time.LocalDateTime

data class SarakeSignResponseRequestDTO(

    var id: String? = null,

    /*
    Request status
    1 = Draft: Request is a Draft and has not been started yet
    2 = Pending: Request is ongoing
    3 = Completed: Request has completed successfully
    4 = Aborted: Request has been aborted
    99 = Failure: Request is in error state due to problems in the process
     */
    var status: Int? = null,

    var finished: LocalDateTime? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SarakeSignResponseRequestDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
