package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties.Arkistointi
import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyCaseProfile
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

internal enum class DynastyDestination(
    val university: YliopistoEnum,
    val fieldPrefix: String,
    val faculty: String,
    val documentLinking: DocumentLinking,
    val archiveProperties: (Arkistointi) -> DynastyArchiveProperties,
    private val profiles: Map<CaseType, DynastyCaseProfile>
) {
    TURKU(
        university = YliopistoEnum.TURUN_YLIOPISTO,
        fieldPrefix = "UTU",
        faculty = "Lääketieteellinen tiedekunta",
        documentLinking = DocumentLinking.TOS_PHASE,
        archiveProperties = Arkistointi::getTurku,
        profiles = mapOf(
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
        documentLinking = DocumentLinking.SAME_PHASE,
        archiveProperties = Arkistointi::getUef,
        profiles = mapOf(
            CaseType.VALMISTUMINEN to DynastyCaseProfile.valmistuminen(
                "03.11", "ELSA valmistumispyyntö ja sen liitteet", "Valmistumisen yhteenveto, EL, EHL tai YEK (valmistumispyyntö)"
            ),
            CaseType.KOEJAKSO to DynastyCaseProfile.koejakso(
                "03.11", "ELSA Koejaksojen yhteenvedot", "Osittain salassa pidettävä", "Asiakirjan päivämäärä (luontipvm)"
            )
        )
    );

    val supportedCaseTypes: Set<CaseType> get() = profiles.keys

    fun profile(caseType: CaseType): DynastyCaseProfile =
        requireNotNull(profiles[caseType]) { "Unsupported Dynasty case type" }

    enum class DocumentLinking { TOS_PHASE, SAME_PHASE }

    companion object {
        fun from(university: YliopistoEnum): DynastyDestination =
            requireNotNull(entries.firstOrNull { it.university == university }) { "Unsupported Dynasty university" }
    }
}
