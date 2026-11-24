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
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.util.UUID

class DynastyDocumentReadbackTest {
    private val mapper = jacksonObjectMapper()
    private val documentIds = listOf("1078", "1079", "1080")
    private val expectedDocumentTypes = listOf("Yhteenveto", "Liite", "Liite")
    private val schemaId = UUID.randomUUID().toString()
    private val assessmentIds = listOf("1078")
    private val expectedPublicityClassName = "Osittain salassa pidettävä"

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

    @Test
    fun `checks the stored publicity class on an archived assessment`() = MockWebServer().use { server ->
        val readback = readback(server)
        server.enqueue(MockResponse().setBody(caseContents(ids = assessmentIds)))
        server.enqueue(MockResponse().setBody(assessment(expectedPublicityClassName)))

        readback.verify("415", assessmentIds, listOf("Lausunto"), expectedPublicityClassName)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Julkinen"])
    fun `rejects an incorrect or missing saved publicity class`(publicityClassName: String?) {
        MockWebServer().use { server ->
            val readback = readback(server)
            server.enqueue(MockResponse().setBody(caseContents(ids = assessmentIds)))
            server.enqueue(MockResponse().setBody(assessment(publicityClassName)))

            assertThatThrownBy { readback.verify("415", assessmentIds, listOf("Lausunto"), expectedPublicityClassName) }
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("publicity class name of document 1078")
        }
    }

    private fun assessment(publicityClassName: String?): String {
        val metadata = mapper.readTree(document(assessmentIds.single(), "Lausunto")) as ObjectNode
        publicityClassName?.let { metadata.put("publicityClassName", it) }
        return metadata.toString()
    }

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
