package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Fixed Turku mapping values; retention lengths and secrecy settings come from configuration. */
internal enum class TurkuDynastyCaseProfile(
    val tosClass: String,
    val task: String,
    val documentTypes: Map<RecordType, String>,
    val publicityClassName: String,
    val retentionRule: String
) {
    VALMISTUMINEN(
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
    KOEJAKSO(
        tosClass = "05.06.02.00",
        task = "Arviointi",
        documentTypes = mapOf(RecordType.ARVIOINTI to "Koejakson vastuuhenkilön arvio"),
        publicityClassName = "Osittain salassapidettävä",
        retentionRule = "Asiakirjan päivämäärä"
    );

    val title: String get() = when (this) {
        VALMISTUMINEN -> "Valmistumisen yhteenveto"
        KOEJAKSO -> "Koejakson vastuuhenkilön arvio"
    }

    val requiresSecrecy: Boolean get() = this == KOEJAKSO

    companion object {
        fun from(caseType: CaseType): TurkuDynastyCaseProfile = when (caseType) {
            CaseType.VALMISTUMINEN -> VALMISTUMINEN
            CaseType.KOEJAKSO -> KOEJAKSO
            else -> throw IllegalArgumentException("Unsupported Dynasty case type")
        }
    }
}
