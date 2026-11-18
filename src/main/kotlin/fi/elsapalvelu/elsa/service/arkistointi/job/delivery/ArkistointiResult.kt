package fi.elsapalvelu.elsa.service.arkistointi.job.delivery

import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError

sealed interface ArkistointiResult {

    data class Success(val externalId: String?) : ArkistointiResult

    data class RetryableFailure(val error: ArkistointiError) : ArkistointiResult

    data class PermanentFailure(val error: ArkistointiError) : ArkistointiResult

    companion object {
        /** The caller classifies retry safety; this factory only constructs the result. */
        fun failure(code: String, description: String, retryable: Boolean): ArkistointiResult {
            val error = ArkistointiError.create(code, description)
            return if (retryable) RetryableFailure(error) else PermanentFailure(error)
        }
    }
}
