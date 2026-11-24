package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.databind.JsonNode
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseId
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

internal class TurkuDynastyGraduationReadback(private val client: DynastyClient, private val documentSchemaId: String) {
    fun verify(caseId: String, documentIds: List<String>) {
        val ids = documentIds.map(::DynastyDocumentId)
        assertThat(ids).hasSize(3).doesNotHaveDuplicates()
        val documents = read(
            "api/v1/cases/${DynastyCaseId(caseId)}/contents", "read_case_contents",
            """{"typeEquals":"DOCUMENT"}""".toRequestBody("application/json".toMediaType())
        )
        assertDirectPhaseLinks(documents, ids)

        ids.forEachIndexed { index, id ->
            val metadata = read("api/v1/documents/$id", "read_document")
            assertThat(metadata.path("id").longValue()).describedAs("document ID").isEqualTo(id.numericValue)
            assertThat(metadata.path("state").asText()).describedAs("state of document $id").isEqualTo("Archived")
            assertThat(metadata.path("schemaID").asText()).describedAs("schema of document $id").isEqualToIgnoringCase(documentSchemaId)
            assertFinnish(metadata, id)
            // Manifest order is summary, combined attachments, licensing certificate.
            if (index > 0) {
                assertThat(metadata.path("documentType").asText()).describedAs("type of document $id").isEqualTo("Liite")
            }
        }
    }

    // The read API field name for the language is not documented, so accept any top-level language field.
    private fun assertFinnish(metadata: JsonNode, id: DynastyDocumentId) {
        val languages = metadata.properties().filter { (name, _) -> name.contains("language", ignoreCase = true) }
            .associate { (name, value) -> name to value.asText() }
        assertThat(languages.values).describedAs("language of document $id, language fields $languages")
            .anyMatch { it.equals(FINNISH, ignoreCase = true) }
    }

    private fun assertDirectPhaseLinks(documents: JsonNode, ids: List<DynastyDocumentId>) {
        assertThat(documents.isArray).describedAs("case contents is an array").isTrue()
        assertThat(documents.map { it.path("id").longValue() })
            .describedAs("documents in the case").containsExactlyInAnyOrderElementsOf(ids.map { it.numericValue })
        val phaseIds = documents.map { document ->
            val id = document.path("id").longValue()
            val parentDocument = document.path("parentCaseEntityId").asText("")
            assertThat(parentDocument).describedAs("parent document of $id").isIn("", EMPTY_GUID.toString())
            val phaseId = UUID.fromString(document.path("parentPhaseId").asText())
            assertThat(phaseId).describedAs("parent phase of document $id").isNotEqualTo(EMPTY_GUID)
            phaseId
        }
        assertThat(phaseIds.distinct()).describedAs("shared parent phase").hasSize(1)
    }

    private fun read(path: String, operation: String, body: RequestBody? = null): JsonNode =
        client.readResource(path, operation, body = body) { response, mapper ->
            check(response.isSuccessful) { "$operation failed with HTTP ${response.code}" }
            val bytes = response.peekBody(MAX_RESPONSE_BYTES + 1).bytes()
            check(bytes.size <= MAX_RESPONSE_BYTES) { "$operation response exceeds the size limit" }
            mapper.readTree(bytes)
        }

    private companion object {
        val EMPTY_GUID = UUID(0, 0)
        const val FINNISH = "fi-FI"
        const val MAX_RESPONSE_BYTES = 1_048_576L
    }
}
