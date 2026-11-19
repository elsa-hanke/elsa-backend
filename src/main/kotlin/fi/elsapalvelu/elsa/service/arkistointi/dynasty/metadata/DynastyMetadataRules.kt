package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailure
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailureCategory
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Document rules shared by startup validation and delivery planning. */
internal class DynastyMetadataRules(private val archiveProperties: DynastyArchiveProperties) {
    private val settings = archiveProperties.dynasty

    fun validateConfiguration(profile: DynastyCaseProfile, configuration: ArkistointiConfiguration) {
        val regulations = settings.tutkintoasetukset
        if (profile.documentTypes.keys.any {
                configuration.metadata.getDocumentMetadata(it, configuration.case)?.retentionPeriod?.toIntOrNull() == null
            } || regulations.isEmpty() || regulations.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
        }
        if (profile.requiresSecrecy) validateSecrecyConfiguration()
    }

    data class DocumentPublicity(val id: String, val secrecyFields: Map<String, String>)

    fun documentPublicity(profile: DynastyCaseProfile): DocumentPublicity {
        if (!profile.requiresSecrecy) return DocumentPublicity("J", emptyMap())
        validateSecrecyConfiguration()
        return DocumentPublicity(requireNotNull(settings.koejaksoPublicityClassId), settings.koejaksoSecrecyFields.toMap())
    }

    private fun validateSecrecyConfiguration() {
        if (settings.koejaksoPublicityClassId.isNullOrBlank() || settings.koejaksoSecrecyFields.size != 3 ||
            settings.koejaksoSecrecyFields.any { (key, value) -> key.isBlank() || value.isBlank() || key in documentFields }) {
            throw DynastyFailure(DynastyFailureCategory.SECRECY_UNCONFIGURED)
        }
    }

    fun retentionPeriod(caseType: CaseType, type: RecordType): Int {
        val metadata = archiveProperties.metadata
        return metadata?.getDocumentMetadata(type, metadata.getCaseMetadata(caseType))?.retentionPeriod?.toIntOrNull()
            ?: throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
    }

    fun degreeRegulation(regulation: String): String = settings.tutkintoasetukset[regulation]
        ?: throw DynastyFailure(DynastyFailureCategory.REGULATION_UNCONFIGURED)

    private val documentFields = setOf(
        "title", "schemaId", "publicityClassId", "publicityClassName", "personalData",
        "documentType", "state", "retentionPeriodRule", "retentionPeriodLength", "AD"
    )
}
