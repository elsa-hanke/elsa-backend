package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailure
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailureCategory
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Document rules shared by startup validation and delivery planning. */
internal class DynastyMetadataRules(private val archiveProperties: DynastyArchiveProperties, private val tosDriven: Boolean) {
    private val settings = archiveProperties.dynasty

    fun validateConfiguration(profile: DynastyCaseProfile, configuration: ArkistointiConfiguration) {
        val regulations = settings.tutkintoasetukset
        val explicitRetentionTypes = if (tosDriven) profile.documentTypes.keys - profile.mainDocumentType else profile.documentTypes.keys
        if (explicitRetentionTypes.any {
                configuration.metadata.getDocumentMetadata(it, configuration.case)?.retentionPeriod?.toIntOrNull() == null
            } || regulations.isEmpty() || regulations.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
        }
        if (!tosDriven && profile.requiresSecrecy) validateSecrecyConfiguration()
    }

    fun publicityClassId(profile: DynastyCaseProfile): String {
        if (!profile.requiresSecrecy) return "J"
        validateSecrecyConfiguration()
        return requireNotNull(settings.koejaksoPublicityClassId)
    }

    private fun validateSecrecyConfiguration() {
        if (settings.koejaksoPublicityClassId.isNullOrBlank()) throw DynastyFailure(DynastyFailureCategory.SECRECY_UNCONFIGURED)
    }

    fun retentionPeriod(caseType: CaseType, type: RecordType): Int {
        val metadata = archiveProperties.metadata
        return metadata?.getDocumentMetadata(type, metadata.getCaseMetadata(caseType))?.retentionPeriod?.toIntOrNull()
            ?: throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
    }

    fun degreeRegulation(regulation: String): String = settings.tutkintoasetukset[regulation]
        ?: throw DynastyFailure(DynastyFailureCategory.REGULATION_UNCONFIGURED)
}
