package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode

internal fun DynastyClient.resolveTosPhase(tosClass: String, phaseTitle: String, documentTitle: String): DynastyTaskId {
    val path = "api/v1/cases/phases/$tosClass"
    val phaseId = readTos(path, "tos_phases") { phases ->
        validateTosClass(phases, tosClass)
        val phase = findTosTitle(phases.path("thsPhases"), phaseTitle)
        try {
            DynastyTaskId(phase.path("taskRowId").textValue() ?: invalidTos(DynastyDiagnosticReason.INVALID_PHASE_ID))
        } catch (_: DynastyFailure) {
            invalidTos(DynastyDiagnosticReason.INVALID_PHASE_ID)
        }
    }

    return readTos("$path/$phaseId/documenttypes", "tos_document_types") { documentTypes ->
        validateTosClass(documentTypes, tosClass)
        val responseId = documentTypes.path("taskRowId").textValue()
        if (!phaseId.toString().equals(responseId, ignoreCase = true)) invalidTos(DynastyDiagnosticReason.PHASE_ID_MISMATCH)
        findTosTitle(documentTypes.path("thsDocuments"), documentTitle)
        phaseId
    }
}

private fun validateTosClass(response: JsonNode, tosClass: String) {
    if (response.path("fullCasegroupCode").textValue() != tosClass) invalidTos(DynastyDiagnosticReason.CASE_CLASS_MISMATCH)
}

// A TOS title must match exactly, including whitespace.
private fun findTosTitle(items: JsonNode, title: String): JsonNode {
    if (!items.isArray) invalidTos(DynastyDiagnosticReason.INVALID_TITLE_LIST)
    val matches = items.filter { it.path("title").textValue() == title }
    if (matches.size > 1) invalidTos(DynastyDiagnosticReason.DUPLICATE_TITLE)
    return matches.singleOrNull() ?: invalidTos(DynastyDiagnosticReason.TITLE_NOT_FOUND)
}

private fun invalidTos(reason: DynastyDiagnosticReason): Nothing = throw DynastyFailure(DynastyFailureCategory.TOS_INVALID, reason)
