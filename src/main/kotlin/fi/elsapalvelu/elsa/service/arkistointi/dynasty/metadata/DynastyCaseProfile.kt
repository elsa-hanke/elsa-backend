package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Fixed Dynasty mapping values; retention lengths and secrecy settings come from configuration. */
internal enum class DynastyCaseProfile(
    val caseType: CaseType,
    val tosClass: String,
    val task: String,
    val documentTypes: Map<RecordType, String>,
    val publicityClassName: String,
    val retentionRule: String
) {
    TURKU_VALMISTUMINEN(
        caseType = CaseType.VALMISTUMINEN,
        tosClass = "05.06.02.04",
        task = "Hakeminen",
        documentTypes = mapOf(
            RecordType.YHTEENVETO to "Valmistumisen yhteenveto, EL, EHL tai YEK",
            RecordType.LIITE to "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK",
            RecordType.LAILLISTAMISTODISTUS to "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK"
        ),
        publicityClassName = "Julkinen",
        retentionRule = "Asian lopullinen ratkaisu"
    ),
    TURKU_KOEJAKSO(
        caseType = CaseType.KOEJAKSO,
        tosClass = "05.06.02.00",
        task = "Arviointi",
        documentTypes = mapOf(RecordType.ARVIOINTI to "Koejakson vastuuhenkilön arvio"),
        publicityClassName = "Osittain salassapidettävä",
        retentionRule = "Asiakirjan päivämäärä"
    ),
    UEF_VALMISTUMINEN(
        caseType = CaseType.VALMISTUMINEN,
        tosClass = "03.11",
        task = "ELSA valmistumispyyntö ja sen liitteet",
        documentTypes = mapOf(
            RecordType.YHTEENVETO to "Valmistumisen yhteenveto, EL, EHL tai YEK (valmistumispyyntö)",
            RecordType.LIITE to "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK",
            RecordType.LAILLISTAMISTODISTUS to "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK"
        ),
        publicityClassName = "Julkinen",
        retentionRule = "Asian lopullinen ratkaisu"
    ),
    UEF_KOEJAKSO(
        caseType = CaseType.KOEJAKSO,
        tosClass = "03.11",
        task = "ELSA Koejaksojen yhteenvedot",
        documentTypes = mapOf(RecordType.ARVIOINTI to "Koejakson vastuuhenkilön arvio"),
        publicityClassName = "Osittain salassa pidettävä",
        retentionRule = "Asiakirjan päivämäärä (luontipvm)"
    );

    val requiresSecrecy: Boolean get() = caseType == CaseType.KOEJAKSO
    val title: String get() = if (requiresSecrecy) "Koejakson vastuuhenkilön arvio" else "Valmistumisen yhteenveto"

    companion object {
        private val profiles = mapOf(
            YliopistoEnum.TURUN_YLIOPISTO to listOf(TURKU_VALMISTUMINEN, TURKU_KOEJAKSO),
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO to listOf(UEF_VALMISTUMINEN, UEF_KOEJAKSO)
        )

        fun from(university: YliopistoEnum, caseType: CaseType): DynastyCaseProfile =
            profiles[university]?.firstOrNull { it.caseType == caseType }
                ?: throw IllegalArgumentException("Unsupported Dynasty case type")
    }
}
