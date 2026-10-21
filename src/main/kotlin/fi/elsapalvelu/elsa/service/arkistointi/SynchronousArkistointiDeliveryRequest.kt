package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

data class SynchronousArkistointiDeliveryRequest(
    val yliopisto: YliopistoEnum,
    val filePath: String,
    val caseType: CaseType,
    val yek: Boolean,
    val caseId: String?,
    val erikoistujanNimi: String?
)
