package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties.Arkistointi
import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.config.arkistointi.archiveConfigurationKey
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyCaseProfile
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.time.LocalDate

internal enum class DynastyArchiveProfile(
    val university: YliopistoEnum,
    val fieldPrefix: String,
    val faculty: String,
    val documentLanguageId: String?,
    val pascalCaseMultipartNames: Boolean,
    val caseRetentionEnd: LocalDate?,
    val includeGraduationExtElsaId: Boolean,
    val attachmentAd: AttachmentAd,
    val archiveProperties: (Arkistointi) -> DynastyArchiveProperties,
    private val caseProfiles: Map<CaseType, DynastyCaseProfile>
) {
    TURKU(
        university = YliopistoEnum.TURUN_YLIOPISTO,
        fieldPrefix = "UTU",
        faculty = "Lääketieteellinen tiedekunta",
        documentLanguageId = "fi-FI",
        pascalCaseMultipartNames = false,
        caseRetentionEnd = null,
        includeGraduationExtElsaId = true,
        attachmentAd = AttachmentAd.TRAINEE_DETAILS,
        archiveProperties = Arkistointi::getTurku,
        caseProfiles = mapOf(
            CaseType.VALMISTUMINEN to DynastyCaseProfile.valmistuminen(
                "05.06.02.04", "Hakeminen", "Valmistumisen yhteenveto,  EL, EHL tai YEK",
                attachmentType = "Valmistumisen yhteenvedon liitteet,  EL, EHL tai YEK"
            ),
            CaseType.KOEJAKSO to DynastyCaseProfile.koejakso(
                "05.06.02.00", "Arviointi"
            )
        )
    ),
    UEF(
        university = YliopistoEnum.ITA_SUOMEN_YLIOPISTO,
        fieldPrefix = "UEF",
        faculty = "Terveystieteiden tiedekunta",
        documentLanguageId = null,
        pascalCaseMultipartNames = true,
        caseRetentionEnd = PERMANENT_RETENTION_END,
        includeGraduationExtElsaId = false,
        attachmentAd = AttachmentAd.LICENSING_DATE_ONLY,
        archiveProperties = Arkistointi::getUef,
        caseProfiles = mapOf(
            CaseType.VALMISTUMINEN to DynastyCaseProfile.valmistuminen(
                "03.11", "ELSA valmistumispyyntö ja sen liitteet",
                "Valmistumisen yhteenveto, EL, EHL tai YEK (valmistumispyyntö)",
                attachmentType = "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK",
                certificateType = "Laillistustodistus (valmistumisen yhteenvedon liite)"
            ),
            CaseType.KOEJAKSO to DynastyCaseProfile.koejakso(
                "03.11", "ELSA Koejaksojen yhteenvedot"
            )
        )
    );

    enum class AttachmentAd { TRAINEE_DETAILS, LICENSING_DATE_ONLY }

    val supportedCaseTypes: Set<CaseType> get() = caseProfiles.keys

    fun caseProfile(caseType: CaseType): DynastyCaseProfile =
        requireNotNull(caseProfiles[caseType]) { "Unsupported Dynasty case type" }

    fun archiveMetadata() = Arkistointi.Metadata().apply {
        cases = caseProfiles.entries.associate { (caseType, profile) ->
            caseType.value to Arkistointi.Case().apply {
                asyncEnabled = true
                documents = profile.documentTypes.keys.associate { it.archiveConfigurationKey to Arkistointi.DocumentMetadata() }
            }
        }
    }

    companion object {
        fun from(university: YliopistoEnum): DynastyArchiveProfile =
            requireNotNull(entries.firstOrNull { it.university == university }) { "Unsupported Dynasty university" }
    }
}

private val PERMANENT_RETENTION_END = LocalDate.of(9999, 12, 31)
