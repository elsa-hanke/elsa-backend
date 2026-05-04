package fi.elsapalvelu.elsa.externalintegration

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory

@Tag("external-integration")
abstract class ExternalIntegrationTestSupport {

    private val log = LoggerFactory.getLogger(javaClass)

    protected fun buildJsonPostRequest(url: String, payload: Any): Request {
        return Request.Builder()
            .url(url)
            .post(objectMapper.writeValueAsString(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    protected fun buildGetRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .build()
    }

    protected fun logRequest(integration: String, endpoint: String, url: String, payload: Any? = null) {
        val renderedPayload = payload?.let { objectMapper.writeValueAsString(it) }
        log.info("{} {} request url={}, payload={}", integration, endpoint, url, renderedPayload)
        println("$integration $endpoint request url=$url, payload=$renderedPayload")
    }

    protected fun logResponse(integration: String, endpoint: String, response: ExternalIntegrationResponse) {
        log.info("{} {} response status={}, body={}", integration, endpoint, response.statusCode, response.body)
        println("$integration $endpoint response status=${response.statusCode}, body=${response.body}")
    }

    protected fun assertSuccessfulResponse(response: ExternalIntegrationResponse) {
        assertThat(response).isNotNull
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isNotBlank()
    }

    protected companion object {
        const val HETU = "030884-227C"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val objectMapper = ObjectMapper()
    }
}

data class ExternalIntegrationResponse(
    val statusCode: Int,
    val body: String?
)

data class HetuRequest(
    val hetu: String
)

data class IdRequest(
    val id: String
)

data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>
)
