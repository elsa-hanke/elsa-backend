package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode

internal fun DynastyClient.resolveTosPhase(tosClass: String, phaseTitle: String, documentTitle: String): DynastyTaskId {
    val path = "api/v1/cases/phases/$tosClass"
    val phases = readTos(path)
    if (phases.path("fullCasegroupCode").textValue() != tosClass) invalidTos()
    val phase = findTosTitle(phases.path("thsPhases"), phaseTitle)
    val phaseId = try {
        DynastyTaskId(phase.path("taskRowId").textValue() ?: invalidTos())
    } catch (_: DynastyFailure) {
        invalidTos()
    }

    val documentTypes = readTos("$path/$phaseId/documenttypes")
    val responseId = documentTypes.path("taskRowId").textValue()
    if (documentTypes.path("fullCasegroupCode").textValue() != tosClass ||
        !phaseId.toString().equals(responseId, ignoreCase = true)) invalidTos()
    findTosTitle(documentTypes.path("thsDocuments"), documentTitle)
    return phaseId
}

// A TOS title must match exactly, including whitespace.
private fun findTosTitle(items: JsonNode, title: String): JsonNode {
    if (!items.isArray) invalidTos()
    return items.singleOrNull { it.path("title").textValue() == title } ?: invalidTos()
}

private fun invalidTos(): Nothing = throw DynastyFailure(DynastyFailureCategory.TOS_INVALID)
