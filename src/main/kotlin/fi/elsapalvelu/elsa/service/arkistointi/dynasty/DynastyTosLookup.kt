package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger(DynastyClient::class.java)

internal fun DynastyClient.resolveTosPhase(tosClass: String, phaseTitle: String, tosTitles: Set<String>): DynastyTaskId {
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

    readTos("$path/$phaseId/documenttypes", "tos_document_types") { documentTypes ->
        validateTosClass(documentTypes, tosClass)
        val responseId = documentTypes.path("taskRowId").textValue()
        if (!phaseId.toString().equals(responseId, ignoreCase = true)) invalidTos(DynastyDiagnosticReason.PHASE_ID_MISMATCH)
        val topLevelDocuments = documentTypes.path("thsDocuments")
        tosTitles.forEach { findTosTitle(topLevelDocuments, it) }
    }
    log.info(
        "Dynasty TOS resolved [tosClass={}, phase={}, taskRowId={}, documentTypes={}]",
        tosClass, phaseTitle, phaseId, tosTitles
    )
    return phaseId
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
