package fi.elsapalvelu.elsa.service.dto.arkistointi

import java.io.Serializable

data class CaseProperties(

    var nativeId: String,

    var type: String,

    var function: String

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
