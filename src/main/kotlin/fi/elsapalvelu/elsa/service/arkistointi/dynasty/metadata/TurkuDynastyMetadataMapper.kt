package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
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
    private companion object {
        const val FACULTY = "Lääketieteellinen tiedekunta"
        val SUPPORTED_SPECIALTY_TYPES = setOf(ErikoisalaTyyppi.LAAKETIEDE, ErikoisalaTyyppi.HAMMASLAAKETIEDE)
    }

    private val configuration = properties.getArkistointi().getTurku()
    private val rules = TurkuDynastyMetadataRules(configuration)

    data class Plan(val tosClass: String, val caseRequest: DynastyCaseRequest, val task: String, val documents: List<DynastyDocumentMetadata>) {
        override fun toString() = "DynastyPlan(documents=${documents.size})"
    }

    fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) = rules.validateConfiguration(caseType, configuration)

    fun plan(submission: ArkistointiSubmission): Plan {
        val payload = reader.read(submission.payload)
        validateDocuments(submission, payload)
        validatePerson(payload)
        val context = MappingContext(payload, submission.caseType, koulutusohjelma(payload))
        val publicity = rules.documentPublicity(context.profile)
        val plan = Plan(
            tosClass = context.profile.tosClass,
            caseRequest = caseRequest(context),
            task = context.profile.task,
            documents = submission.asiakirjat.map { documentMetadata(context, publicity, it.asiakirjatyyppi) }
        )
        validateSerialization(plan)
        return plan
    }

    private fun validateDocuments(submission: ArkistointiSubmission, payload: ArchivePayload) {
        require(payload.university == submission.university.name) { "university mismatch" }
        val documents = submission.asiakirjat
        ArkistointiManifestValidator.validate(payload.documents, documents)
        // Dynasty attaches documents in this order; SÄHKE sorts records during package creation.
        val order = documents.map { it.jarjestysnumero }
        require(order == order.sorted()) { "document order" }
        documents.forEach { document ->
            require(document.sisalto.isNotEmpty()) { "empty document content" }
            require(document.filename.isNotBlank()) { "missing document filename" }
            document.contentType.toMediaType()
        }
        require(documents.map { it.asiakirjatyyppi } == rules.expectedDocumentTypes(submission.caseType)) { "document types" }
    }

    private fun validatePerson(payload: ArchivePayload) {
        require(specialtyType(payload) in SUPPORTED_SPECIALTY_TYPES) { "unsupported specialty type" }
        require(!payload.firstName.isNullOrBlank()) { "missing first name" }
        require(!payload.lastName.isNullOrBlank()) { "missing last name" }
        require(!payload.specialtyName.isNullOrBlank()) { "missing specialty name" }
    }

    private fun specialtyType(payload: ArchivePayload): ErikoisalaTyyppi? = ErikoisalaTyyppi.entries.firstOrNull { it.name == payload.specialtyType }

    private enum class Koulutusohjelma(val prefix: String, val title: String) {
        EL("EL", "Erikoislääkärikoulutus"),
        EHL("EHL", "Erikoishammaslääkärikoulutus"),
        YEK("YEK", "Yleislääketieteen erityiskoulutus")
    }

    private fun koulutusohjelma(payload: ArchivePayload): Koulutusohjelma = when {
        payload.yek -> Koulutusohjelma.YEK
        specialtyType(payload) == ErikoisalaTyyppi.HAMMASLAAKETIEDE -> Koulutusohjelma.EHL
        else -> Koulutusohjelma.EL
    }

    // Keep the default Object.toString(): this context contains personal data.
    private class MappingContext(val payload: ArchivePayload, val caseType: CaseType, val koulutusohjelma: Koulutusohjelma) {
        val profile = TurkuDynastyCaseProfile.from(caseType)
        val isValmistuminen = profile == TurkuDynastyCaseProfile.VALMISTUMINEN
        val name = "${payload.firstName} ${payload.lastName}"
        val date: String = payload.approvalDate.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
        val title = "${koulutusohjelma.prefix} ${profile.title} $name $date"
    }

    private fun caseRequest(context: MappingContext) = DynastyCaseRequest(DynastyCaseModel(
        schemaId = configuration.dynasty.caseSchemaId,
        title = context.title,
        additionalData = additionalData(context, birthDate = context.payload.birthDate?.toString())
    ))

    private fun additionalData(
        context: MappingContext,
        birthDate: String? = null,
        studyRightStartDate: String? = null,
        graduationDate: String? = null,
        approvalDate: String? = null,
        degreeRegulation: String? = null,
        licensingDate: String? = null
    ) = DynastyAdditionalData(
        firstName = context.payload.firstName, lastName = context.payload.lastName,
        faculty = FACULTY, program = context.koulutusohjelma.title, specialty = context.payload.specialtyName,
        birthDate = birthDate, studyRightStartDate = studyRightStartDate,
        graduationDate = graduationDate, approvalDate = approvalDate,
        degreeRegulation = degreeRegulation, licensingDate = licensingDate
    )

    private fun documentMetadata(
        context: MappingContext,
        publicity: TurkuDynastyMetadataRules.DocumentPublicity,
        type: RecordType
    ): DynastyDocumentMetadata {
        val settings = configuration.dynasty
        val additionalData = documentAdditionalData(context, type)
        val retention = rules.retentionPeriod(context.caseType, type)
        return DynastyDocumentMetadata(
            title = documentTitle(context, type),
            schemaId = settings.documentSchemaId,
            documentType = context.profile.documentTypes.getValue(type),
            retentionPeriodLength = retention,
            additionalData = additionalData,
            publicityClassId = publicity.id,
            publicityClassName = context.profile.publicityClassName,
            retentionPeriodRule = context.profile.retentionRule,
            secrecyFields = publicity.secrecyFields
        )
    }

    private fun documentAdditionalData(context: MappingContext, type: RecordType): DynastyAdditionalData {
        val payload = context.payload
        val main = type in setOf(RecordType.YHTEENVETO, RecordType.ARVIOINTI)
        return additionalData(
            context,
            birthDate = if (main) payload.birthDate?.toString() else null,
            studyRightStartDate = if (main) payload.studyRightStartDate?.toString() else null,
            graduationDate = if (main && context.isValmistuminen) payload.approvalDate.toString() else null,
            approvalDate = if (main && !context.isValmistuminen) payload.approvalDate.toString() else null,
            degreeRegulation = if (main) payload.degreeRegulation?.let { regulation ->
                rules.degreeRegulation(regulation)
            } else null,
            licensingDate = if (context.isValmistuminen && type != RecordType.LIITE) payload.licensingDate?.toString() else null
        )
    }

    private fun documentTitle(context: MappingContext, type: RecordType): String = when (type) {
        RecordType.LIITE -> "Valmistumisen yhteenvedon liitteet ${context.name} ${context.date}"
        RecordType.LAILLISTAMISTODISTUS -> "Laillistustodistus ${context.name}"
        else -> context.title
    }

    private fun validateSerialization(plan: Plan) {
        // Validate the entire immutable plan before any external mutation.
        mapper.writeValueAsString(plan.caseRequest)
        plan.documents.forEach { mapper.writeValueAsString(it) }
    }
}
