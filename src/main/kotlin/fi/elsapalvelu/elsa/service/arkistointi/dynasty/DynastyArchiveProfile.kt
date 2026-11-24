package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties.Arkistointi
import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyCaseProfile
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

internal enum class DynastyArchiveProfile(
    val university: YliopistoEnum,
    val fieldPrefix: String,
    val faculty: String,
    val phaseSelection: PhaseSelection,
    val archiveProperties: (Arkistointi) -> DynastyArchiveProperties,
    private val caseProfiles: Map<CaseType, DynastyCaseProfile>
) {
    TURKU(
        university = YliopistoEnum.TURUN_YLIOPISTO,
        fieldPrefix = "UTU",
        faculty = "Lääketieteellinen tiedekunta",
        phaseSelection = PhaseSelection.TOS,
        archiveProperties = Arkistointi::getTurku,
        caseProfiles = mapOf(
            CaseType.VALMISTUMINEN to DynastyCaseProfile.valmistuminen(
                "05.06.02.04", "Hakeminen", "Valmistumisen yhteenveto,  EL, EHL tai YEK"
            ),
            CaseType.KOEJAKSO to DynastyCaseProfile.koejakso(
                "05.06.02.00", "Arviointi", "Osittain salassapidettävä", "Asiakirjan päivämäärä"
            )
        )
    ),
    UEF(
        university = YliopistoEnum.ITA_SUOMEN_YLIOPISTO,
        fieldPrefix = "UEF",
        faculty = "Terveystieteiden tiedekunta",
        phaseSelection = PhaseSelection.EXPLICIT,
        archiveProperties = Arkistointi::getUef,
        caseProfiles = mapOf(
            CaseType.VALMISTUMINEN to DynastyCaseProfile.valmistuminen(
                "03.11", "ELSA valmistumispyyntö ja sen liitteet", "Yhteenveto", attachmentType = "Liite"
            ),
            CaseType.KOEJAKSO to DynastyCaseProfile.koejakso(
                "03.11", "ELSA Koejaksojen yhteenvedot", "Osittain salassa pidettävä", "Asiakirjan päivämäärä (luontipvm)",
                assessmentType = "Lausunto"
            )
        )
    );

    val supportedCaseTypes: Set<CaseType> get() = caseProfiles.keys

    fun caseProfile(caseType: CaseType): DynastyCaseProfile =
        requireNotNull(caseProfiles[caseType]) { "Unsupported Dynasty case type" }

    enum class PhaseSelection { TOS, EXPLICIT }

    companion object {
        fun from(university: YliopistoEnum): DynastyArchiveProfile =
            requireNotNull(entries.firstOrNull { it.university == university }) { "Unsupported Dynasty university" }
    }
}
