package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseRequest
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseModel
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseRetentionPeriod
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyAdditionalData
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastySecrecyPeriod
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyTosDocumentMetadata
import fi.elsapalvelu.elsa.config.arkistointi.DynastyArchiveProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailure
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyFailureCategory
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
    }

    private val settings = archiveProperties.dynasty

    data class Plan(val tosClass: String, val caseRequest: DynastyCaseRequest, val task: String, val documents: List<DynastyTosDocumentMetadata>) {
        override fun toString() = "DynastyPlan(documents=${documents.size})"
    }

    fun validateConfiguration() {
        val regulations = settings.tutkintoasetukset
        if (regulations.isEmpty() || regulations.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
        }
    }

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

    private fun documentMetadata(context: MappingContext, type: RecordType) = DynastyTosDocumentMetadata(
        documentTitle(context, type), settings.documentSchemaId, documentAdditionalData(context, type),
        if (context.profile.requiresSecrecy) secrecyPeriod() else null,
        archiveProfile.documentLanguageId
    )

    private fun secrecyPeriod(): DynastySecrecyPeriod {
        val received = LocalDate.now(clock.withZone(ARCHIVE_ZONE))
        return DynastySecrecyPeriod(received, received.plusYears(KOEJAKSO_SECRECY_YEARS.toLong()))
    }

    private fun documentAdditionalData(context: MappingContext, type: RecordType): DynastyAdditionalData? =
        if (type == context.profile.mainDocumentType) mainDocumentAdditionalData(context)
        else attachmentAdditionalData(context, type)

    private fun mainDocumentAdditionalData(context: MappingContext): DynastyAdditionalData {
        val payload = context.payload
        val regulation = payload.degreeRegulation?.let {
            settings.tutkintoasetukset[it] ?: throw DynastyFailure(DynastyFailureCategory.REGULATION_UNCONFIGURED)
        }
        val decisionDate = payload.approvalDate.toString()
        return additionalData(
            context,
            birthDate = payload.birthDate?.toString(),
            studyRightStartDate = payload.studyRightStartDate?.toString(),
            graduationDate = decisionDate.takeIf { context.isValmistuminen },
            approvalDate = decisionDate.takeUnless { context.isValmistuminen },
            degreeRegulation = regulation,
            licensingDate = payload.licensingDate?.toString()?.takeIf { context.isValmistuminen },
            extElsaId = payload.caseId.toString().takeIf { archiveProfile == DynastyArchiveProfile.TURKU && context.isValmistuminen }
        )
    }

    private fun attachmentAdditionalData(context: MappingContext, type: RecordType): DynastyAdditionalData? {
        val licensingDate = context.payload.licensingDate?.toString()?.takeIf { type == RecordType.LAILLISTAMISTODISTUS }
        return if (archiveProfile == DynastyArchiveProfile.UEF) {
            licensingDate?.let { DynastyAdditionalData(archiveProfile.fieldPrefix, licensingDate = it) }
        } else {
            additionalData(context, licensingDate = licensingDate)
        }
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
