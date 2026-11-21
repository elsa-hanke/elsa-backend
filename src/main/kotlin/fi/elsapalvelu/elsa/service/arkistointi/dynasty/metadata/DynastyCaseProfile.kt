package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Fixed mapping values. Retention lengths and secrecy fields come from configuration. */
internal class DynastyCaseProfile(
    val tosClass: String,
    val task: String,
    val title: String,
    val mainDocumentType: RecordType,
    val documentTypes: Map<RecordType, String>,
    val publicityClassName: String,
    val requiresSecrecy: Boolean,
    val retentionRule: String
) {
    companion object {
        private const val ASSESSMENT_TITLE = "Koejakson vastuuhenkilön arvio"
        private const val GRADUATION_ATTACHMENTS = "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK"

        fun valmistuminen(tosClass: String, task: String, summaryType: String, attachmentType: String = GRADUATION_ATTACHMENTS) = DynastyCaseProfile(
            tosClass = tosClass,
            task = task,
            title = "Valmistumisen yhteenveto",
            mainDocumentType = RecordType.YHTEENVETO,
            documentTypes = mapOf(
                RecordType.YHTEENVETO to summaryType,
                RecordType.LIITE to attachmentType,
                RecordType.LAILLISTAMISTODISTUS to attachmentType
            ),
            publicityClassName = "Julkinen",
            requiresSecrecy = false,
            retentionRule = "Asian lopullinen ratkaisu"
        )

        fun koejakso(
            tosClass: String, task: String, publicityClassName: String, retentionRule: String, assessmentType: String = ASSESSMENT_TITLE
        ) = DynastyCaseProfile(
            tosClass = tosClass,
            task = task,
            title = ASSESSMENT_TITLE,
            mainDocumentType = RecordType.ARVIOINTI,
            documentTypes = mapOf(RecordType.ARVIOINTI to assessmentType),
            publicityClassName = publicityClassName,
            requiresSecrecy = true,
            retentionRule = retentionRule
        )
    }
}
