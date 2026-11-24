package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Metadata DTOs deliberately keep Object.toString(): diagnostics must not contain personal data.
class DynastyCaseRequest(val caseModel: DynastyCaseModel)

class DynastyCaseModel(
    @get:JsonProperty("schemaid") val schemaId: String?,
    val title: String,
    @get:JsonProperty("AD") val additionalData: DynastyAdditionalData,
    val publicityClassId: String = "J",
    val publicityClassName: String = "Julkinen",
    val manner: String = "M",
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val retentionPeriodLength: Int? = null,
    @get:JsonProperty("RetentionPeriod")
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val retentionPeriod: DynastyCaseRetentionPeriod? = null
)

class DynastyCaseRetentionPeriod(end: LocalDate) {
    @get:JsonProperty("End")
    val end: String = end.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

class DynastyTaskRequest(val title: String)

class DynastyAdditionalData(
    fieldPrefix: String,
    firstName: String? = null,
    lastName: String? = null,
    faculty: String? = null,
    program: String? = null,
    specialty: String? = null,
    birthDate: String? = null,
    studyRightStartDate: String? = null,
    graduationDate: String? = null,
    approvalDate: String? = null,
    degreeRegulation: String? = null,
    licensingDate: String? = null,
    extElsaId: String? = null
) {
    @get:JsonAnyGetter
    val fields = buildMap {
        firstName?.let { put("${fieldPrefix}etunimi", it) }
        lastName?.let { put("${fieldPrefix}sukunimi", it) }
        faculty?.let { put("${fieldPrefix}tiedekunta", it) }
        program?.let { put("${fieldPrefix}koulutusohjelma", it) }
        specialty?.let { put("${fieldPrefix}paaaine", it) }
        birthDate?.let { put("${fieldPrefix}syntymaaika", it) }
        studyRightStartDate?.let { put("${fieldPrefix}alkamispaiva", it) }
        graduationDate?.let { put("${fieldPrefix}valmistumispaiva", it) }
        approvalDate?.let { put("${fieldPrefix}hyvaksymispaiva", it) }
        degreeRegulation?.let { put("${fieldPrefix}tutkintoasetus", it) }
        licensingDate?.let { put("${fieldPrefix}laillistamispaiva", it) }
        extElsaId?.let { put("ExtElsaTunniste", it) }
    }
}

sealed interface DynastyDocumentRequestMetadata

class DynastyDocumentMetadata(
    val title: String,
    val schemaId: String?,
    val documentType: String,
    val retentionPeriodLength: Int,
    @get:JsonProperty("AD") @get:JsonInclude(JsonInclude.Include.NON_NULL) val additionalData: DynastyAdditionalData?,
    val publicityClassId: String = "J",
    val publicityClassName: String = "Julkinen",
    val retentionPeriodRule: String = "Asian lopullinen ratkaisu",
    val personalData: String = "Sisältää henkilötietoja",
    val state: Int = 3,
    @get:JsonProperty("SecrecyPeriod") @get:JsonInclude(JsonInclude.Include.NON_NULL) val secrecyPeriod: DynastySecrecyPeriod? = null,
    @get:JsonProperty("DeliveryType") @get:JsonInclude(JsonInclude.Include.NON_NULL) val deliveryType: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val secrecyPeriodRule: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val secrecyPeriodLength: Int? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val secrecyReason: List<String>? = null
) : DynastyDocumentRequestMetadata

class DynastyTosDocumentMetadata(
    val title: String,
    val schemaId: String?,
    @get:JsonProperty("AD") val additionalData: DynastyAdditionalData,
    @get:JsonProperty("SecrecyPeriod") @get:JsonInclude(JsonInclude.Include.NON_NULL) val secrecyPeriod: DynastySecrecyPeriod?
) : DynastyDocumentRequestMetadata {
    @get:JsonProperty("DeliveryType")
    val deliveryType = "M"
    val state = 3
}

class DynastySecrecyPeriod(start: LocalDate, end: LocalDate) {
    @get:JsonProperty("Start")
    val start: String = start.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @get:JsonProperty("End")
    val end: String = end.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

sealed interface DynastyCaseAttachmentOptions

class DynastyExplicitPhaseOptions(caseId: DynastyCaseId, phaseId: DynastyTaskId) : DynastyCaseAttachmentOptions {
    @get:JsonProperty("CaseSelector") val caseSelector = DynastyTextSelector(caseId.toString())
    @get:JsonProperty("PhaseSelector") val phaseSelector = DynastyTextSelector(phaseId.toString())
}

class DynastyTosPhaseOptions(caseId: DynastyCaseId, phaseId: DynastyTaskId, tosTitle: String) : DynastyCaseAttachmentOptions {
    @get:JsonProperty("CaseSelector")
    val caseSelector = DynastyTosCaseSelector(caseId.numericValue)

    @get:JsonProperty("PhaseSelector")
    val phaseSelector = DynastyTosPhaseSelector(DynastySmartAttachOptions(listOf(DynastyTextSelector(phaseId.toString()))))

    @get:JsonProperty("TOSTitle")
    val tosTitle = tosTitle
}

class DynastyTosCaseSelector(@get:JsonProperty("Id") val id: Long)
class DynastyTosPhaseSelector(val smartAttachOptions: DynastySmartAttachOptions)
class DynastySmartAttachOptions(val pathSelector: List<DynastyTextSelector>)

class DynastyTextSelector(@get:JsonProperty("Id") val id: String)
