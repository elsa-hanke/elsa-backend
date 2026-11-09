package fi.elsapalvelu.elsa.service.arkistointi.dynasty

/** Local Dynasty policies; persisted codes stay stable even when categories have different retry rules. */
internal enum class DynastyFailureCategory(
    val code: String,
    val retryable: Boolean,
    val definitelyNotProcessed: Boolean
) {
    CONFIGURATION_INVALID("DYNASTY_CONFIGURATION_INVALID", retryable = false, definitelyNotProcessed = false),
    SECRECY_UNCONFIGURED("DYNASTY_SECRECY_UNCONFIGURED", retryable = false, definitelyNotProcessed = false),
    REGULATION_UNCONFIGURED("DYNASTY_REGULATION_UNCONFIGURED", retryable = false, definitelyNotProcessed = false),
    AUTH_FAILED("DYNASTY_AUTH_FAILED", retryable = false, definitelyNotProcessed = true),
    AUTH_TEMPORARILY_REJECTED("DYNASTY_AUTH_FAILED", retryable = true, definitelyNotProcessed = true),
    AUTH_UNAVAILABLE("DYNASTY_AUTH_UNAVAILABLE", retryable = true, definitelyNotProcessed = true),
    CONNECTION_FAILED("DYNASTY_UNAVAILABLE", retryable = true, definitelyNotProcessed = true),
    TLS_FAILED("DYNASTY_UNAVAILABLE", retryable = false, definitelyNotProcessed = true),
    HTTP_REJECTED("DYNASTY_HTTP_REJECTED", retryable = false, definitelyNotProcessed = true),
    DELIVERY_UNCONFIRMED("DYNASTY_DELIVERY_UNCONFIRMED", retryable = false, definitelyNotProcessed = false),
    RESPONSE_INVALID("DYNASTY_RESPONSE_INVALID", retryable = false, definitelyNotProcessed = false),
    STEP_IN_PROGRESS("DYNASTY_STEP_IN_PROGRESS", retryable = true, definitelyNotProcessed = false)
}

/** Safe diagnostics only: no original exception, response body or request data. */
internal class DynastyFailure(val category: DynastyFailureCategory) : RuntimeException("Dynasty request failed [${category.code}]")
