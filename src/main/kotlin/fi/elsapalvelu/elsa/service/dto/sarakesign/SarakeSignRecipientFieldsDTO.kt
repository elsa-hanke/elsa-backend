package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable

data class SarakeSignRecipientFieldsDTO(

    var firstName: String? = null,

    var lastName: String? = null,

    var title: String? = null,

    var organizationName: String? = null,

    var languageCode: String? = null,

    var phoneNumber: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SarakeSignRecipientFieldsDTO

        if (title != other.title) return false

        return true
    }

    override fun hashCode() = 31
}
