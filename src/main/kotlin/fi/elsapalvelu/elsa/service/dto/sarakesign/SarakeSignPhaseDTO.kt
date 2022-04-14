package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable

data class SarakeSignPhaseDTO(

    var phaseNumber: Long? = null,

    var title: String? = null,

    var description: String? = null,

    var roleTitle: String? = null,

    var roleDescription: String? = null,

    var ordered: Boolean? = null,

    var acceptThreshold: Long? = null,

    var finishingThreshold: Long? = null,

    var allowReject: Boolean? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SarakeSignPhaseDTO

        if (title != other.title) return false

        return true
    }

    override fun hashCode() = 31
}
