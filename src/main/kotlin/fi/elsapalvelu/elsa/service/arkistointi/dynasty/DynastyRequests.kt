package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.annotation.JsonAnyGetter
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
    licensingDate: String? = null
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
    }
}

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
