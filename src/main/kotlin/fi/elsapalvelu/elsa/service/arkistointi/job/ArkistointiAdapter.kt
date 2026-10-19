package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface ArkistointiAdapter {
    val university: YliopistoEnum

    /**
     * Atomically deduplicate deliveries by pyynto.key, including concurrent requests and retries
     * after an uncertain remote success. A non-atomic check followed by a create is insufficient.
     * Error codes/descriptions must not contain personal data, filenames, credentials or response bodies.
     */
    fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult
}
