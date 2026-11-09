package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// Metadata DTOs deliberately keep Object.toString(): diagnostics must not contain personal data.
class DynastyCaseRequest(val caseModel: DynastyCaseModel)

class DynastyCaseModel(
    @get:JsonProperty("schemaid") val schemaId: String?,
    val title: String,
    @get:JsonProperty("AD") val additionalData: DynastyAdditionalData,
    val publicityClassId: String = "J",
    val publicityClassName: String = "Julkinen",
    val manner: String = "M"
)

class DynastyTaskRequest(val title: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
class DynastyAdditionalData(
    @get:JsonProperty("UTUetunimi") val firstName: String? = null,
    @get:JsonProperty("UTUsukunimi") val lastName: String? = null,
    @get:JsonProperty("UTUtiedekunta") val faculty: String? = null,
    @get:JsonProperty("UTUkoulutusohjelma") val program: String? = null,
    @get:JsonProperty("UTUpaaaine") val specialty: String? = null,
    @get:JsonProperty("UTUsyntymaaika") val birthDate: String? = null,
    @get:JsonProperty("UTUalkamispaiva") val studyRightStartDate: String? = null,
    @get:JsonProperty("UTUvalmistumispaiva") val graduationDate: String? = null,
    @get:JsonProperty("UTUhyvaksymispaiva") val approvalDate: String? = null,
    @get:JsonProperty("UTUtutkintoasetus") val degreeRegulation: String? = null,
    @get:JsonProperty("UTUlaillistamispaiva") val licensingDate: String? = null
)

class DynastyDocumentMetadata(
    val title: String,
    val schemaId: String?,
    val documentType: String,
    val retentionPeriodLength: Int,
    @get:JsonProperty("AD") val additionalData: DynastyAdditionalData,
    val publicityClassId: String = "J",
    val publicityClassName: String = "Julkinen",
    val retentionPeriodRule: String = "Asian lopullinen ratkaisu",
    val personalData: String = "Sisältää henkilötietoja",
    val state: Int = 3,
    @get:JsonAnyGetter val secrecyFields: Map<String, String> = emptyMap()
)

sealed interface DynastyDocumentOptions

class DynastyMainDocumentOptions(caseId: DynastyCaseId, taskId: DynastyTaskId) : DynastyDocumentOptions {
    @get:JsonProperty("CaseSelector") val caseSelector = DynastyTextSelector(caseId.toString())
    @get:JsonProperty("PhaseSelector") val phaseSelector = DynastyTextSelector(taskId.toString())
}

class DynastyAttachmentOptions(caseId: DynastyCaseId, parentId: DynastyDocumentId) : DynastyDocumentOptions {
    val caseSelector = DynastyNumericSelector(caseId.numericValue)
    val parentDocumentSelector = DynastyNumericSelector(parentId.numericValue)
}

class DynastyTextSelector(@get:JsonProperty("Id") val id: String)
class DynastyNumericSelector(val id: Long)
