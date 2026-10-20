package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface ArkistointiAdapter {
    val university: YliopistoEnum

    /**
     * Deduplicate by the persisted pyynto.key UUID, including concurrent calls and retries after an uncertain success.
     * The key is the saved job UUID (pyynto.jobId). Serialize it as the canonical UUID string, not a business key.
     * Deduplication must be atomic at the receiving system; a separate lookup followed by create is insufficient.
     * A manual retry retains the original key. This contract provides at-least-once delivery, not exactly-once.
     * Error codes/descriptions must not contain personal data, filenames, credentials or response bodies.
     * Success.externalId is null or a nonblank, well-formed Unicode string of at most 255 UTF-16
     * code units (matching the entity's Size validation), without NUL. The ID is stored unchanged,
     * including any leading/trailing whitespace; it is never truncated, trimmed or hashed.
     * An invalid success response requires manual reconciliation: the archive may already have accepted it.
     * Treat each document's sisalto as read-only. Mutating it changes the current submission;
     * a retry always reloads the immutable bytes from the database.
     */
    fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult
}
