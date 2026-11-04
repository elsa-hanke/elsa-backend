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
    data class Plan(val tosClass: String, val caseRequest: DynastyCaseRequest, val task: String, val documents: List<DynastyDocumentMetadata>) {
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
        val caseRequest = DynastyCaseRequest(DynastyCaseModel(c.caseSchemaId, title, DynastyAdditionalData(
            firstName = p.firstName, lastName = p.lastName, faculty = "Lääketieteellinen tiedekunta",
            program = program, specialty = p.specialtyName, birthDate = p.birthDate?.toString()
        )))
        val metadata = properties.getArkistointi().getTurku().metadata
        val documents = docs.map { doc ->
            val main = doc.asiakirjatyyppi in setOf(RecordType.YHTEENVETO,RecordType.ARVIOINTI)
            val ad = DynastyAdditionalData(
                firstName = p.firstName, lastName = p.lastName, faculty = "Lääketieteellinen tiedekunta",
                program = program, specialty = p.specialtyName,
                birthDate = if (main) p.birthDate?.toString() else null,
                studyRightStartDate = if (main) p.studyRightStartDate?.toString() else null,
                graduationDate = if (main && graduation) p.approvalDate.toString() else null,
                approvalDate = if (main && !graduation) p.approvalDate.toString() else null,
                degreeRegulation = if (main) p.degreeRegulation?.let { regulation ->
                    c.tutkintoasetukset[regulation] ?: throw DynastyFailure("DYNASTY_REGULATION_UNCONFIGURED")
                } else null,
                licensingDate = if (graduation && doc.asiakirjatyyppi != RecordType.LIITE) p.licensingDate?.toString() else null
            )
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
            if (!graduation) validateSecrecyConfiguration()
            DynastyDocumentMetadata(
                title = documentTitle, schemaId = c.documentSchemaId, documentType = documentType,
                retentionPeriodLength = retention, additionalData = ad,
                publicityClassId = if (graduation) "J" else requireNotNull(c.koejaksoPublicityClassId),
                publicityClassName = if (graduation) "Julkinen" else "Osittain salassa pidettävä",
                retentionPeriodRule = if (graduation) "Asian lopullinen ratkaisu" else "Asiakirjan päivämäärä",
                secrecyFields = if (graduation) emptyMap() else c.koejaksoSecrecyFields.toMap()
            )
        }
        // Validate serialization of the immutable plan before any external mutation.
        mapper.writeValueAsString(caseRequest)
        documents.forEach { mapper.writeValueAsString(it) }
        return Plan(if (graduation) "05.06.02.04" else "05.06.02.00",caseRequest,if (graduation) "Hakeminen" else "Arviointi",documents)
    }
}
