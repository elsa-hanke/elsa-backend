package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailure
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailureCategory
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Turku-specific document and configuration rules, shared by startup validation and delivery planning. */
internal class TurkuDynastyMetadataRules(private val turku: ApplicationProperties.Arkistointi.Turku) {
    fun expectedDocumentTypes(caseType: CaseType): List<RecordType> = when (caseType) {
        CaseType.VALMISTUMINEN -> listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
        CaseType.KOEJAKSO -> listOf(RecordType.ARVIOINTI)
        else -> throw IllegalArgumentException("Unsupported Dynasty case type")
    }

    fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        val regulations = turku.dynasty.tutkintoasetukset
        if (expectedDocumentTypes(caseType).any {
                configuration.metadata.getDocumentMetadata(it, configuration.case)?.retentionPeriod?.toIntOrNull() == null
            } || regulations.isEmpty() || regulations.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
        }
        if (caseType != CaseType.VALMISTUMINEN) validateSecrecyConfiguration()
    }

    fun validateSecrecyConfiguration() {
        val settings = turku.dynasty
        if (settings.koejaksoPublicityClassId.isNullOrBlank() || settings.koejaksoSecrecyFields.size != 3 ||
            settings.koejaksoSecrecyFields.any { (key, value) -> key.isBlank() || value.isBlank() || key in documentFields }) {
            throw DynastyFailure(DynastyFailureCategory.SECRECY_UNCONFIGURED)
        }
    }

    fun retentionPeriod(caseType: CaseType, type: RecordType): Int {
        val metadata = turku.metadata
        return metadata?.getDocumentMetadata(type, metadata.getCaseMetadata(caseType))?.retentionPeriod?.toIntOrNull()
            ?: throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
    }

    fun degreeRegulation(regulation: String): String = turku.dynasty.tutkintoasetukset[regulation]
        ?: throw DynastyFailure(DynastyFailureCategory.REGULATION_UNCONFIGURED)

    private val documentFields = setOf(
        "title", "schemaId", "publicityClassId", "publicityClassName", "personalData",
        "documentType", "state", "retentionPeriodRule", "retentionPeriodLength", "AD"
    )
}
