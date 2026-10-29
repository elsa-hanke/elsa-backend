package fi.elsapalvelu.elsa.service.arkistointi.job.delivery

import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError

sealed interface ArkistointiResult {

    data class Success(val externalId: String?) : ArkistointiResult

    data class RetryableFailure(val error: ArkistointiError) : ArkistointiResult

    data class PermanentFailure(val error: ArkistointiError) : ArkistointiResult
}
