package fi.elsapalvelu.elsa.service.dto.arkistointi

import fi.elsapalvelu.elsa.domain.Asiakirja
import java.io.Serializable

data class RecordProperties(

    var asiakirja: Asiakirja,

    var retentionPeriod: String,

    var type: String

) : Serializable
