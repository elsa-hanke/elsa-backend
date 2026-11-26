package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Titles must match the university’s TOS configuration. */
internal class DynastyCaseProfile(
    val tosClass: String,
    val task: String,
    val title: String,
    val mainDocumentType: RecordType,
    val documentTypes: Map<RecordType, String>,
    val requiresSecrecy: Boolean
) {
    companion object {
        private const val ASSESSMENT_TITLE = "Koejakson vastuuhenkilön arvio"

        fun valmistuminen(
            tosClass: String, task: String, summaryType: String, attachmentType: String, certificateType: String = attachmentType
        ) = DynastyCaseProfile(
            tosClass = tosClass,
            task = task,
            title = "Valmistumisen yhteenveto",
            mainDocumentType = RecordType.YHTEENVETO,
            documentTypes = mapOf(
                RecordType.YHTEENVETO to summaryType,
                RecordType.LIITE to attachmentType,
                RecordType.LAILLISTAMISTODISTUS to certificateType
            ),
            requiresSecrecy = false
        )

        fun koejakso(tosClass: String, task: String) = DynastyCaseProfile(
            tosClass = tosClass,
            task = task,
            title = ASSESSMENT_TITLE,
            mainDocumentType = RecordType.ARVIOINTI,
            documentTypes = mapOf(RecordType.ARVIOINTI to ASSESSMENT_TITLE),
            requiresSecrecy = true
        )
    }
}
