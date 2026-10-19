package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface ArkistointiAdapter {
    val university: YliopistoEnum

    /**
     * Atomically deduplicate deliveries by pyynto.key, including concurrent requests and retries
     * after an uncertain remote success. A non-atomic check followed by a create is insufficient.
     * Error codes/descriptions must not contain personal data, filenames, credentials or response bodies.
     * Success.externalId is null or a nonblank, well-formed Unicode string of at most 255 UTF-16
     * code units (matching the entity's Size validation), without NUL. The ID is stored unchanged,
     * including any leading/trailing whitespace; it is never truncated, trimmed or hashed.
     * An invalid success response requires manual reconciliation: the archive may already have accepted it.
     */
    fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult
}
