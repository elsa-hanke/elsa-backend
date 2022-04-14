package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable

data class SarakeSignRecipientDTO(

    var phaseNumber: Long? = null,

    var recipient: String? = null,

    var readonly: String? = null,

    var fields: SarakeSignRecipientFieldsDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SarakeSignRecipientDTO

        if (phaseNumber != other.phaseNumber) return false

        return true
    }

    override fun hashCode() = 31
}
