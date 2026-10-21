package fi.elsapalvelu.elsa.service.arkistointi.job

/** Only safe, adapter-sanitized descriptions are allowed here. Never attach the original cause. */
class ArkistointiProcessingException(
    val error: ArkistointiError,
    val retryable: Boolean = true
) : RuntimeException(error.code)
