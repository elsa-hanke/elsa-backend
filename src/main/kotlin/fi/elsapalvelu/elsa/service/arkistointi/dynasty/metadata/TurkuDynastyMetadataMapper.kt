package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseRequest
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseModel
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyAdditionalData
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentMetadata
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiManifestValidator
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import okhttp3.MediaType.Companion.toMediaType
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/** Build and serialize the entire delivery before creating anything in Dynasty. */
@Component
class TurkuDynastyMetadataMapper(
    properties: ApplicationProperties,
    private val mapper: ObjectMapper,
    private val reader: ArchivePayloadReader
) {
    private val configuration = properties.getArkistointi().getTurku()
    private val rules = TurkuDynastyMetadataRules(configuration)

    data class Plan(val tosClass: String, val caseRequest: DynastyCaseRequest, val task: String, val documents: List<DynastyDocumentMetadata>) {
        override fun toString() = "DynastyPlan(documents=${documents.size})"
    }

    /** Validate static settings only; snapshot values are checked by plan(). */
    fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) {
        rules.validateConfiguration(caseType, configuration)
    }

    fun plan(submission: ArkistointiSubmission): Plan {
        val payload = reader.read(submission.payload)
        validateDocuments(submission, payload)
        validatePerson(payload)
        val context = MappingContext(payload, submission.caseType, educationProgram(payload))
        val plan = Plan(
            tosClass = if (context.graduation) "05.06.02.04" else "05.06.02.00",
            caseRequest = caseRequest(context),
            task = if (context.graduation) "Hakeminen" else "Arviointi",
            documents = submission.asiakirjat.map { documentMetadata(context, it.asiakirjatyyppi) }
        )
        validateSerialization(plan)
        return plan
    }

    private fun validateDocuments(submission: ArkistointiSubmission, payload: ArchivePayload) {
        require(payload.university == submission.university.name)
        val documents = submission.asiakirjat
        ArkistointiManifestValidator.validate(payload.documents, documents)
        // Dynasty attaches documents in this order; SÄHKE sorts records during package creation.
        val order = documents.map { it.jarjestysnumero }
        require(order == order.sorted())
        documents.forEach { document ->
            require(document.sisalto.isNotEmpty() && document.filename.isNotBlank())
            document.contentType.toMediaType()
        }
        require(documents.map { it.asiakirjatyyppi } == rules.expectedDocumentTypes(submission.caseType))
    }

    private fun validatePerson(payload: ArchivePayload) {
        require(payload.specialtyType in setOf("LAAKETIEDE", "HAMMASLAAKETIEDE"))
        require(!payload.firstName.isNullOrBlank() && !payload.lastName.isNullOrBlank() && !payload.specialtyName.isNullOrBlank())
    }

    private enum class EducationProgram(val prefix: String, val title: String) {
        EL("EL", "Erikoislääkärikoulutus"),
        EHL("EHL", "Erikoishammaslääkärikoulutus"),
        YEK("YEK", "Yleislääketieteen erityiskoulutus")
    }

    private fun educationProgram(payload: ArchivePayload): EducationProgram = when {
        payload.yek -> EducationProgram.YEK
        payload.specialtyType == "HAMMASLAAKETIEDE" -> EducationProgram.EHL
        else -> EducationProgram.EL
    }

    // Keep the default Object.toString(): this context contains personal data.
    private class MappingContext(val payload: ArchivePayload, val caseType: CaseType, val program: EducationProgram) {
        val graduation = caseType == CaseType.VALMISTUMINEN
        val name = "${payload.firstName} ${payload.lastName}"
        val date = payload.approvalDate.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
        val title = "${program.prefix} ${if (graduation) "Valmistumisen yhteenveto" else "Koejakson vastuuhenkilön arvio"} $name $date"
    }

    private fun caseRequest(context: MappingContext): DynastyCaseRequest {
        val payload = context.payload
        return DynastyCaseRequest(DynastyCaseModel(
            schemaId = configuration.dynasty.caseSchemaId,
            title = context.title,
            additionalData = DynastyAdditionalData(
                firstName = payload.firstName, lastName = payload.lastName,
                faculty = "Lääketieteellinen tiedekunta", program = context.program.title,
                specialty = payload.specialtyName, birthDate = payload.birthDate?.toString()
            )
        ))
    }

    private fun documentMetadata(context: MappingContext, type: RecordType): DynastyDocumentMetadata {
        val settings = configuration.dynasty
        val additionalData = documentAdditionalData(context, type)
        val retention = rules.retentionPeriod(context.caseType, type)
        if (!context.graduation) rules.validateSecrecyConfiguration()
        return DynastyDocumentMetadata(
            title = documentTitle(context, type),
            schemaId = settings.documentSchemaId,
            documentType = documentType(context, type),
            retentionPeriodLength = retention,
            additionalData = additionalData,
            publicityClassId = if (context.graduation) "J" else requireNotNull(settings.koejaksoPublicityClassId),
            publicityClassName = if (context.graduation) "Julkinen" else "Osittain salassa pidettävä",
            retentionPeriodRule = if (context.graduation) "Asian lopullinen ratkaisu" else "Asiakirjan päivämäärä",
            secrecyFields = if (context.graduation) emptyMap() else settings.koejaksoSecrecyFields.toMap()
        )
    }

    private fun documentAdditionalData(context: MappingContext, type: RecordType): DynastyAdditionalData {
        val payload = context.payload
        val main = type in setOf(RecordType.YHTEENVETO, RecordType.ARVIOINTI)
        return DynastyAdditionalData(
            firstName = payload.firstName, lastName = payload.lastName,
            faculty = "Lääketieteellinen tiedekunta", program = context.program.title, specialty = payload.specialtyName,
            birthDate = if (main) payload.birthDate?.toString() else null,
            studyRightStartDate = if (main) payload.studyRightStartDate?.toString() else null,
            graduationDate = if (main && context.graduation) payload.approvalDate.toString() else null,
            approvalDate = if (main && !context.graduation) payload.approvalDate.toString() else null,
            degreeRegulation = if (main) payload.degreeRegulation?.let { regulation ->
                rules.degreeRegulation(regulation)
            } else null,
            licensingDate = if (context.graduation && type != RecordType.LIITE) payload.licensingDate?.toString() else null
        )
    }

    private fun documentTitle(context: MappingContext, type: RecordType): String = when (type) {
        RecordType.LIITE -> "Valmistumisen yhteenvedon liitteet ${context.name} ${context.date}"
        RecordType.LAILLISTAMISTODISTUS -> "Laillistustodistus ${context.name}"
        else -> context.title
    }

    private fun documentType(context: MappingContext, type: RecordType): String = when {
        !context.graduation -> "Koejakson vastuuhenkilön arvio"
        type == RecordType.YHTEENVETO -> "Valmistumisen yhteenveto, EL, EHL tai YEK"
        else -> "Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK"
    }

    private fun validateSerialization(plan: Plan) {
        // Validate the entire immutable plan before any external mutation.
        mapper.writeValueAsString(plan.caseRequest)
        plan.documents.forEach { mapper.writeValueAsString(it) }
    }
}
