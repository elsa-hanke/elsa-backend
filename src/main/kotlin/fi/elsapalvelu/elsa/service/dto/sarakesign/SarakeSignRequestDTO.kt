package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable
import java.time.LocalDateTime

data class SarakeSignRequestDTO(

    var title: String? = null,

    var description: String? = null,

    var dueDate: LocalDateTime? = null,

    var notificationDays: Long? = null,

    var autoArchive: Boolean? = null,

    var archiveDescription: String? = null,

    var sendMail: Boolean? = null,

    var combineDocuments: Boolean? = null,

    var confidentiality: Long? = null,

    var validProofTypes: List<Long>? = null,

    var proofScope: Long? = null,

    var managers: List<String>? = null,

    var phases: List<SarakeSignPhaseDTO>? = null,

    var recipients: List<SarakeSignRecipientDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SarakeSignRequestDTO

        if (title != other.title) return false

        return true
    }

    override fun hashCode() = 31
}
