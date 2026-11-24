package fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyCaseId
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDocumentId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

internal class DynastyDocumentReadback(
    private val client: DynastyClient,
    private val documentSchemaId: String,
    private val expectedLanguageId: String? = null
) {
    data class KoejaksoSecrecy(
        val deliveryDates: ClosedRange<LocalDate>,
        val publicityClassId: String? = null,
        val publicityClassName: String? = null
    ) {
        init {
            require(!publicityClassId.isNullOrBlank() || !publicityClassName.isNullOrBlank())
        }
    }

    fun verify(
        caseId: String, documentIds: List<String>, expectedDocumentTypes: List<String?>, expectedSecrecy: KoejaksoSecrecy? = null
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
            expectedLanguageId?.let { assertLanguage(metadata, id, it) }
            expectedSecrecy?.let { assertSecrecy(metadata, id, it) }
        }
    }

    private fun assertLanguage(metadata: JsonNode, id: DynastyDocumentId, expected: String) {
        val language = metadata.field("languageId") ?: metadata.field("language")
        assertThat(language?.textValue()).describedAs("language of document $id").isEqualToIgnoringCase(expected)
    }

    private fun assertSecrecy(metadata: JsonNode, id: DynastyDocumentId, expected: KoejaksoSecrecy) {
        expected.publicityClassId?.let {
            assertThat(metadata.field("publicityClassId")?.textValue()).describedAs("publicity class ID of document $id").isEqualTo(it)
        }
        expected.publicityClassName?.let {
            assertThat(metadata.field("publicityClassName")?.textValue()).describedAs("publicity class name of document $id").isEqualTo(it)
        }

        val period = metadata.field("secrecyPeriod")
        val start = readDateTime(period?.field("start") ?: metadata.field("secrecyPeriodStart"), "secrecy start of document $id")
        val end = readDateTime(period?.field("end") ?: metadata.field("secrecyPeriodEnd"), "secrecy end of document $id")
        assertThat(start.toLocalDate()).describedAs("secrecy start date of document $id")
            .isBetween(expected.deliveryDates.start, expected.deliveryDates.endInclusive)
        assertThat(start.toLocalTime()).describedAs("secrecy start time of document $id").isEqualTo(LocalTime.MIDNIGHT)
        assertThat(end).describedAs("secrecy end of document $id").isEqualTo(start.plusYears(100))
    }

    private fun readDateTime(value: JsonNode?, description: String): LocalDateTime {
        val text = value?.textValue()
        assertThat(text).describedAs(description).isNotBlank()
        val dateTime = try {
            DateTimeFormatter.ISO_DATE_TIME.parseBest(text, ZonedDateTime::from, LocalDateTime::from)
        } catch (exception: DateTimeParseException) {
            throw AssertionError("$description must be an ISO date-time", exception)
        }
        return if (dateTime is ZonedDateTime) dateTime.withZoneSameInstant(DYNASTY_ARCHIVE_ZONE).toLocalDateTime()
        else LocalDateTime.from(dateTime)
    }

    private fun JsonNode.field(name: String): JsonNode? =
        get(name) ?: properties().firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

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

internal val DYNASTY_ARCHIVE_ZONE: ZoneId = ZoneId.of("Europe/Helsinki")
