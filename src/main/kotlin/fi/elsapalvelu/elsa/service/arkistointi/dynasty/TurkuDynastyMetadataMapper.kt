package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiManifestValidator
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/** Build and serialize the entire delivery before creating anything in Dynasty. */
@Component
class TurkuDynastyMetadataMapper(
    private val properties: ApplicationProperties,
    private val mapper: ObjectMapper,
    private val reader: ArchivePayloadReader
) {
    data class Plan(val tosClass: String, val caseJson: String, val task: String, val documents: List<String>) {
        override fun toString() = "DynastyPlan(documents=${documents.size})"
    }

    /** Validate static settings only; snapshot values are checked by plan(). */
    fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        val c = properties.getArkistointi().getTurku().dynasty
        val graduation = caseType == CaseType.VALMISTUMINEN
        val types = if (graduation) listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
            else listOf(RecordType.ARVIOINTI)
        if (types.any {
                configuration.metadata.getDocumentMetadata(it, configuration.case)?.retentionPeriod?.toIntOrNull() == null
            } || c.tutkintoasetukset.isEmpty() || c.tutkintoasetukset.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID")
        }
        if (!graduation) validateSecrecyConfiguration()
    }

    private fun validateSecrecyConfiguration() {
        val c = properties.getArkistointi().getTurku().dynasty
        if (c.koejaksoPublicityClassId.isNullOrBlank() || c.koejaksoSecrecyFields.size != 3 ||
            c.koejaksoSecrecyFields.any { (key, value) -> key.isBlank() || value.isBlank() || key in documentFields }) {
            throw DynastyFailure("DYNASTY_SECRECY_UNCONFIGURED")
        }
    }

    private val documentFields = setOf("title", "schemaId", "publicityClassId", "publicityClassName", "personalData",
        "documentType", "state", "retentionPeriodRule", "retentionPeriodLength", "AD")

    @Suppress("CyclomaticComplexMethod", "LongMethod") // Explicit field-by-field customer mapping; validate all before delivery.
    fun plan(submission: ArkistointiSubmission): Plan {
        val c = properties.getArkistointi().getTurku().dynasty
        val p = reader.read(submission.payload)
        require(p.university == submission.university.name)
        val docs = submission.asiakirjat
        ArkistointiManifestValidator.validate(p.documents, docs)
        // Dynasty attaches documents in this order; SÄHKE sorts its records during package creation.
        require(docs.map { it.jarjestysnumero } == docs.map { it.jarjestysnumero }.sorted())
        docs.forEach { document ->
            require(document.sisalto.isNotEmpty() && document.filename.isNotBlank())
            okhttp3.MediaType.Companion.run { document.contentType.toMediaType() }
        }
        val graduation = submission.caseType == CaseType.VALMISTUMINEN
        require(if (graduation) docs.map { it.asiakirjatyyppi } == listOf(RecordType.YHTEENVETO,RecordType.LIITE,RecordType.LAILLISTAMISTODISTUS)
            else submission.caseType == CaseType.KOEJAKSO && docs.map { it.asiakirjatyyppi } == listOf(RecordType.ARVIOINTI))
        val specialtyType = p.specialtyType
        require(specialtyType in setOf("LAAKETIEDE", "HAMMASLAAKETIEDE"))
        require(!p.firstName.isNullOrBlank() && !p.lastName.isNullOrBlank() && !p.specialtyName.isNullOrBlank())
        val prefix = if (p.yek) "YEK" else if (specialtyType == "HAMMASLAAKETIEDE") "EHL" else "EL"
        val program = when(prefix) { "YEK" -> "Yleislääketieteen erityiskoulutus"; "EHL" -> "Erikoishammaslääkärikoulutus"; else -> "Erikoislääkärikoulutus" }
        val name = "${p.firstName} ${p.lastName}"
        val date = p.approvalDate.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
        val title = "$prefix ${if (graduation) "Valmistumisen yhteenveto" else "Koejakson vastuuhenkilön arvio"} $name $date"
        val common = linkedMapOf("UTUetunimi" to p.firstName, "UTUsukunimi" to p.lastName,
            "UTUtiedekunta" to "Lääketieteellinen tiedekunta", "UTUkoulutusohjelma" to program, "UTUpaaaine" to p.specialtyName)
        val caseAd = common.toMutableMap().apply { p.birthDate?.let { put("UTUsyntymaaika",it.toString()) } }
        val caseJson = mapper.writeValueAsString(mapOf("caseModel" to mapOf("schemaid" to c.caseSchemaId,
            "title" to title,"publicityClassId" to "J","publicityClassName" to "Julkinen","manner" to "M","AD" to caseAd)))
        val metadata = properties.getArkistointi().getTurku().metadata
        val documents = docs.map { doc ->
            val main = doc.asiakirjatyyppi in setOf(RecordType.YHTEENVETO,RecordType.ARVIOINTI)
            val ad = common.toMutableMap()
            if (main) {
                p.birthDate?.let { ad["UTUsyntymaaika"] = it.toString() }
                p.studyRightStartDate?.let { ad["UTUalkamispaiva"] = it.toString() }
                ad[if (graduation) "UTUvalmistumispaiva" else "UTUhyvaksymispaiva"] = p.approvalDate.toString()
                p.degreeRegulation?.let { regulation ->
                    ad["UTUtutkintoasetus"] = c.tutkintoasetukset[regulation] ?: throw DynastyFailure("DYNASTY_REGULATION_UNCONFIGURED")
                }
            }
            if (graduation && doc.asiakirjatyyppi != RecordType.LIITE) p.licensingDate?.let { ad["UTUlaillistamispaiva"] = it.toString() }
            val retention = metadata?.getDocumentMetadata(doc.asiakirjatyyppi, metadata.getCaseMetadata(submission.caseType))?.retentionPeriod?.toIntOrNull()
                ?: throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID")
            val documentTitle = when(doc.asiakirjatyyppi) {
                RecordType.LIITE -> "Valmistumisen yhteenvedon liitteet $name $date"
                RecordType.LAILLISTAMISTODISTUS -> "Laillistustodistus $name"
                else -> title
            }
            val documentType = when {
                !graduation -> "Koejakson vastuuhenkilön arvio"
                main -> "Valmistumisen yhteenveto, EL, EHL tai YEK"
                else -> "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK"
            }
            val result = linkedMapOf<String,Any?>("title" to documentTitle,"schemaId" to c.documentSchemaId,
                "publicityClassId" to "J","publicityClassName" to "Julkinen","personalData" to "Sisältää henkilötietoja",
                "documentType" to documentType,"state" to 3,"retentionPeriodRule" to "Asian lopullinen ratkaisu",
                "retentionPeriodLength" to retention,"AD" to ad)
            if (!graduation) {
                validateSecrecyConfiguration()
                result["publicityClassId"] = c.koejaksoPublicityClassId
                result["publicityClassName"] = "Osittain salassa pidettävä"
                result["retentionPeriodRule"] = "Asiakirjan päivämäärä"
                result.putAll(c.koejaksoSecrecyFields)
            }
            mapper.writeValueAsString(result)
        }
        return Plan(if (graduation) "05.06.02.04" else "05.06.02.00",caseJson,if (graduation) "Hakeminen" else "Arviointi",documents)
    }
}
