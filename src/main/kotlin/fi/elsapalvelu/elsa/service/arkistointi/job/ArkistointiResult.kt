package fi.elsapalvelu.elsa.service.arkistointi.job

sealed interface ArkistointiResult {

    data class Success(
        val externalId: String?
    ) : ArkistointiResult

    data class RetryableFailure(
        val error: ArkistointiError
    ) : ArkistointiResult

    data class PermanentFailure(
        val error: ArkistointiError
    ) : ArkistointiResult
}

class ArkistointiError private constructor(
    val code: String,
    val description: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArkistointiError) return false

        return code == other.code && description == other.description
    }

    override fun hashCode(): Int = 31 * code.hashCode() + description.hashCode()

    override fun toString(): String = "ArkistointiError(code='$code')"

    companion object {
        const val MAX_ERROR_CODE_LENGTH = 100
        const val MAX_ERROR_DESCRIPTION_LENGTH = 1000

        fun create(code: String, description: String): ArkistointiError {
            require(code.isNotBlank()) { "ArkistointiError code must not be blank" }
            return ArkistointiError(
                code = code.take(MAX_ERROR_CODE_LENGTH),
                description = description.take(MAX_ERROR_DESCRIPTION_LENGTH)
            )
        }
    }
}
