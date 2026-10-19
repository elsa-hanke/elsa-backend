package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface ArkistointiAdapter {
    val university: YliopistoEnum

    /**
     * Deduplicate deliveries by pyynto.key, including retries after an uncertain remote success.
     * Error codes/descriptions must not contain personal data, filenames, credentials or response bodies.
     */
    fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult
}
