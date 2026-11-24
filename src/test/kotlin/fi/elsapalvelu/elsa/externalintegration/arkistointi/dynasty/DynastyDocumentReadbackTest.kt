package fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DYNASTY_TEST_TOKEN_RESPONSE
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.dynastyTestProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class DynastyDocumentReadbackTest {
    private val mapper = jacksonObjectMapper()
    private val documentIds = listOf("1078", "1079", "1080")
    private val expectedDocumentTypes = listOf("Yhteenveto", "Liite", "Liite")
    private val schemaId = UUID.randomUUID().toString()
    private val assessmentIds = listOf("1078")
    private val deliveryDate = LocalDate.of(2026, 11, 19)
    private val expectedSecrecy = DynastyDocumentReadback.KoejaksoSecrecy(
        deliveryDate..deliveryDate, publicityClassId = "S", publicityClassName = "Osittain salassapidettävä"
    )

    @Test
    fun `reads stored documents belonging directly to the same phase`() = MockWebServer().use { server ->
        val readback = readback(server)
        server.enqueue(MockResponse().setBody(caseContents()))
        documentIds.forEachIndexed { index, id ->
            val type = if (index == 0) "Yhteenveto" else "Liite"
            server.enqueue(MockResponse().setBody(document(id, type)))
        }

        readback.verify("415", documentIds, expectedDocumentTypes)

        assertThat(server.requestCount).isEqualTo(5)
        assertThat(server.takeRequest().path).isEqualTo("/token")
        val contents = server.takeRequest()
        assertThat(contents.method).isEqualTo("POST")
        assertThat(contents.path).isEqualTo("/api/v1/cases/415/contents")
        assertThat(mapper.readTree(contents.body.readUtf8())).isEqualTo(mapper.readTree("""{"typeEquals":"DOCUMENT"}"""))
        documentIds.forEach { id ->
            val document = server.takeRequest()
            assertThat(document.method).isEqualTo("GET")
            assertThat(document.path).isEqualTo("/api/v1/documents/$id")
            assertThat(document.getHeader("Authorization")).isEqualTo("Bearer test")
        }
    }

    @Test
    fun `an attachment stored under the summary fails the readback`() {
        MockWebServer().use { server ->
            val readback = readback(server)
            server.enqueue(MockResponse().setBody(caseContents(parentDocumentId = UUID.randomUUID().toString())))

            assertThatThrownBy { readback.verify("415", documentIds, expectedDocumentTypes) }
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("parent document of 1079")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `checks stored secrecy on an archived assessment`(flatPeriod: Boolean) = MockWebServer().use { server ->
        val readback = readback(server)
        val metadata = assessment()
        if (flatPeriod) {
            val period = metadata.remove("secrecyPeriod")
            metadata.put("secrecyPeriodStart", period.path("start").asText())
            metadata.put("secrecyPeriodEnd", period.path("end").asText())
        }
        server.enqueue(MockResponse().setBody(caseContents(ids = assessmentIds)))
        server.enqueue(MockResponse().setBody(metadata.toString()))

        readback.verify("415", assessmentIds, listOf("Lausunto"), expectedSecrecy)
    }

    @ParameterizedTest
    @CsvSource(
        "publicityClassID,J,publicity class ID",
        "publicityClassName,Julkinen,publicity class name",
        "start,2026-11-18T00:00:00,secrecy start date",
        "start,2026-11-19T01:00:00,secrecy start time",
        "end,2125-11-19T00:00:00,secrecy end",
        "end,,secrecy end"
    )
    fun `rejects incorrect or missing saved metadata`(field: String, replacement: String?, description: String) {
        MockWebServer().use { server ->
            val readback = readback(server)
            val metadata = assessment()
            val period = metadata.path("secrecyPeriod") as ObjectNode
            val target = if (period.has(field)) period else metadata
            if (replacement == null) target.remove(field) else target.put(field, replacement)
            server.enqueue(MockResponse().setBody(caseContents(ids = assessmentIds)))
            server.enqueue(MockResponse().setBody(metadata.toString()))

            assertThatThrownBy { readback.verify("415", assessmentIds, listOf("Lausunto"), expectedSecrecy) }
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("$description of document 1078")
        }
    }

    private fun assessment(): ObjectNode = mapper.readTree(
        """{
            "id":1078,"state":"Archived","schemaID":"$schemaId","documentType":"Lausunto",
            "publicityClassID":"S","publicityClassName":"Osittain salassapidettävä",
            "secrecyPeriod":{"start":"2026-11-18T22:00:00Z","end":"2126-11-19T00:00:00"}
        }"""
    ) as ObjectNode

    private fun document(id: String, type: String) =
        """{"id":$id,"state":"Archived","schemaID":"$schemaId","documentType":"$type"}"""

    private fun caseContents(parentDocumentId: String? = null, ids: List<String> = documentIds): String {
        val phaseId = UUID.randomUUID()
        return ids.mapIndexed { index, id ->
            val parent = if (index == 1 && parentDocumentId != null) "\"$parentDocumentId\"" else "null"
            """{"id":$id,"type":"DOCUMENT","parentPhaseId":"$phaseId","parentCaseEntityId":$parent}"""
        }.joinToString(prefix = "[", postfix = "]")
    }

    private fun readback(server: MockWebServer): DynastyDocumentReadback {
        val settings = dynastyTestProperties(server.url("/").newBuilder().host("127.0.0.1").build().toString())
            .getArkistointi().getTurku().dynasty
        server.enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
        return DynastyDocumentReadback(DynastyClient(settings, mapper, Clock.systemUTC()), schemaId)
    }
}
