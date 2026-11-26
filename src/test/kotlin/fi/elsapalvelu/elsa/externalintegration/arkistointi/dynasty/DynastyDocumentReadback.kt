package fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseId
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

internal class DynastyDocumentReadback(private val client: DynastyClient, private val documentSchemaId: String) {
    fun verify(
        caseId: String, documentIds: List<String>, expectedDocumentTypes: List<String?>, expectedPublicityClassName: String? = null
    ) {
        val ids = documentIds.map(::DynastyDocumentId)
        assertThat(ids).hasSize(expectedDocumentTypes.size).doesNotHaveDuplicates()
        val documents = read(
            "api/v1/cases/${DynastyCaseId(caseId)}/contents", "read_case_contents",
            """{"typeEquals":"DOCUMENT"}""".toRequestBody("application/json".toMediaType())
        )
        assertDirectPhaseLinks(documents, ids)

        ids.zip(expectedDocumentTypes).forEach { (id, expectedType) ->
            val metadata = read("api/v1/documents/$id", "read_document")
            assertThat(metadata.path("id").longValue()).describedAs("document ID").isEqualTo(id.numericValue)
            assertThat(metadata.path("state").asText()).describedAs("state of document $id").isEqualTo("Archived")
            assertThat(metadata.path("schemaID").asText()).describedAs("schema of document $id").isEqualToIgnoringCase(documentSchemaId)
            if (expectedType != null) {
                assertThat(metadata.path("documentType").asText()).describedAs("type of document $id").isEqualTo(expectedType)
            }
            // Dynasty exposes the saved class name. DynastyDeliveryIT checks the outgoing secrecy period.
            expectedPublicityClassName?.let {
                assertThat(metadata.path("publicityClassName").textValue()).describedAs("publicity class name of document $id").isEqualTo(it)
            }
        }
    }

    fun verifyCaseRetentionBasis(caseId: String, expectedBasis: String) {
        val metadata = read("api/v1/cases/${DynastyCaseId(caseId)}", "read_case")
        assertThat(metadata.path("retentionPeriodBasis").map { it.asText() })
            .describedAs("retention basis of case $caseId").contains(expectedBasis)
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
        const val MAX_RESPONSE_BYTES = 1_048_576L
    }
}
