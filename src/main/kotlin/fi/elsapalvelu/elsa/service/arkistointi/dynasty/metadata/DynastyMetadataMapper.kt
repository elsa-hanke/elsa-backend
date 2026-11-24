package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseRequest
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseModel
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseRetentionPeriod
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyAdditionalData
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentMetadata
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentRequestMetadata
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastySecrecyPeriod
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyTosDocumentMetadata
import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiManifestValidator
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayload
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import okhttp3.MediaType.Companion.toMediaType
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Build and serialize the entire delivery before creating anything in Dynasty. */
open class DynastyMetadataMapper internal constructor(
    private val archiveProfile: DynastyArchiveProfile,
    archiveProperties: DynastyArchiveProperties,
    private val mapper: ObjectMapper,
    private val reader: ArchivePayloadReader,
    private val clock: Clock
) {
    private companion object {
        val SUPPORTED_SPECIALTY_TYPES = setOf(ErikoisalaTyyppi.LAAKETIEDE, ErikoisalaTyyppi.HAMMASLAAKETIEDE)
        val ARCHIVE_ZONE: ZoneId = ZoneId.of("Europe/Helsinki")
        val PERMANENT_RETENTION_END: LocalDate = LocalDate.of(9999, 12, 31)
        const val KOEJAKSO_SECRECY_YEARS = 100
        const val UEF_SECRECY_REASON = "JulkL 24.1 § 30 kohta (henk. koht. omin, oppilashuolto)"
    }

    private val settings = archiveProperties.dynasty
    private val tosDriven = archiveProfile.phaseSelection == DynastyArchiveProfile.PhaseSelection.TOS
    private val rules = DynastyMetadataRules(archiveProperties, tosDriven)

    data class Plan(val tosClass: String, val caseRequest: DynastyCaseRequest, val task: String, val documents: List<DynastyDocumentRequestMetadata>) {
        override fun toString() = "DynastyPlan(documents=${documents.size})"
    }

    fun validateConfiguration(caseType: CaseType, configuration: ArkistointiConfiguration) =
        rules.validateConfiguration(archiveProfile.caseProfile(caseType), configuration)

    fun plan(submission: ArkistointiSubmission): Plan {
        val payload = reader.read(submission.payload)
        val profile = archiveProfile.caseProfile(submission.caseType)
        validateDocuments(submission, payload, profile)
        validatePerson(payload)
        val context = MappingContext(payload, submission.caseType, profile, koulutusohjelma(payload))
        val plan = Plan(
            tosClass = context.profile.tosClass,
            caseRequest = caseRequest(context),
            task = context.profile.task,
            documents = submission.asiakirjat.map { documentMetadata(context, it.asiakirjatyyppi) }
        )
        validateSerialization(plan)
        return plan
    }

    private fun validateDocuments(
        submission: ArkistointiSubmission, payload: ArchivePayload, profile: DynastyCaseProfile
    ) {
        require(payload.university == submission.university.name) { "university mismatch" }
        val documents = submission.asiakirjat
        ArkistointiManifestValidator.validate(payload.documents, documents)
        // Dynasty creates documents in manifest order; SÄHKE sorts records during package creation.
        val order = documents.map { it.jarjestysnumero }
        require(order == order.sorted()) { "document order" }
        documents.forEach { document ->
            require(document.sisalto.isNotEmpty()) { "empty document content" }
            require(document.filename.isNotBlank()) { "missing document filename" }
            document.contentType.toMediaType()
        }
        require(documents.map { it.asiakirjatyyppi } == profile.documentTypes.keys.toList()) { "document types" }
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
    private class MappingContext(
        val payload: ArchivePayload,
        val caseType: CaseType,
        val profile: DynastyCaseProfile,
        val koulutusohjelma: Koulutusohjelma
    ) {
        val isValmistuminen = caseType == CaseType.VALMISTUMINEN
        val name = "${payload.firstName} ${payload.lastName}"
        val date: String = payload.approvalDate.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
        val title = "${koulutusohjelma.prefix} ${profile.title} $name $date"
    }

    private fun caseRequest(context: MappingContext) = DynastyCaseRequest(DynastyCaseModel(
        schemaId = settings.caseSchemaId,
        title = context.title,
        additionalData = additionalData(context, birthDate = context.payload.birthDate?.toString()),
        retentionPeriodLength = if (archiveProfile == DynastyArchiveProfile.UEF) -1 else null,
        retentionPeriod = if (archiveProfile == DynastyArchiveProfile.UEF) DynastyCaseRetentionPeriod(PERMANENT_RETENTION_END) else null
    ))

    private fun additionalData(
        context: MappingContext,
        birthDate: String? = null,
        studyRightStartDate: String? = null,
        graduationDate: String? = null,
        approvalDate: String? = null,
        degreeRegulation: String? = null,
        licensingDate: String? = null,
        extElsaId: String? = null
    ) = DynastyAdditionalData(
        firstName = context.payload.firstName, lastName = context.payload.lastName,
        faculty = archiveProfile.faculty,
        program = context.koulutusohjelma.title, specialty = context.payload.specialtyName,
        birthDate = birthDate, studyRightStartDate = studyRightStartDate,
        graduationDate = graduationDate, approvalDate = approvalDate,
        degreeRegulation = degreeRegulation, licensingDate = licensingDate, extElsaId = extElsaId,
        fieldPrefix = archiveProfile.fieldPrefix
    )

    private fun documentMetadata(context: MappingContext, type: RecordType): DynastyDocumentRequestMetadata {
        if (tosDriven && type == context.profile.mainDocumentType) {
            val secrecyPeriod = if (context.profile.requiresSecrecy) secrecyPeriod() else null
            return DynastyTosDocumentMetadata(context.title, settings.documentSchemaId, documentAdditionalData(context, type), secrecyPeriod)
        }
        val publicityClassId = rules.publicityClassId(context.profile)
        val additionalData = documentAdditionalData(context, type)
        val retention = rules.retentionPeriod(context.caseType, type)
        val confidential = context.profile.requiresSecrecy
        return DynastyDocumentMetadata(
            title = documentTitle(context, type),
            schemaId = settings.documentSchemaId,
            documentType = if (tosDriven) "Liite" else context.profile.documentTypes.getValue(type),
            retentionPeriodLength = retention,
            additionalData = additionalData,
            publicityClassId = publicityClassId,
            publicityClassName = context.profile.publicityClassName,
            retentionPeriodRule = context.profile.retentionRule,
            secrecyPeriod = if (confidential) secrecyPeriod() else null,
            deliveryType = "M",
            secrecyPeriodRule = "Asiakirjan päivämäärä".takeIf { confidential },
            secrecyPeriodLength = KOEJAKSO_SECRECY_YEARS.takeIf { confidential },
            secrecyReason = listOf(UEF_SECRECY_REASON).takeIf { confidential }
        )
    }

    private fun secrecyPeriod(): DynastySecrecyPeriod {
        val received = LocalDate.now(clock.withZone(ARCHIVE_ZONE))
        return DynastySecrecyPeriod(received, received.plusYears(KOEJAKSO_SECRECY_YEARS.toLong()))
    }

    private fun documentAdditionalData(context: MappingContext, type: RecordType): DynastyAdditionalData {
        val payload = context.payload
        val main = type == context.profile.mainDocumentType
        return additionalData(
            context,
            birthDate = if (main) payload.birthDate?.toString() else null,
            studyRightStartDate = if (main) payload.studyRightStartDate?.toString() else null,
            graduationDate = if (main && context.isValmistuminen) payload.approvalDate.toString() else null,
            approvalDate = if (main && !context.isValmistuminen) payload.approvalDate.toString() else null,
            degreeRegulation = if (main) payload.degreeRegulation?.let { regulation ->
                rules.degreeRegulation(regulation)
            } else null,
            licensingDate = if (context.isValmistuminen && type != RecordType.LIITE) payload.licensingDate?.toString() else null,
            extElsaId = if (main && tosDriven && context.isValmistuminen) payload.caseId.toString() else null
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
