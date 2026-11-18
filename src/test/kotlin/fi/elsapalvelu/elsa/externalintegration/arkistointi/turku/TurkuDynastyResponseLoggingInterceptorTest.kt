package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TurkuDynastyResponseLoggingInterceptorTest {
    @Test
    fun `logs endpoints and responses without exposing credentials or consuming bodies`() {
        MockWebServer().use { server ->
            val tokenUrl = server.url("/connect/token")
            val casesUrl = server.url("/api/v1/cases?tosClass=test")
            val settings = DynastyProperties().apply {
                this.tokenUrl = tokenUrl.toString()
                clientId = "test-client-id"
                clientSecret = "test-client-secret"
            }
            val output = mutableListOf<String>()
            val client = OkHttpClient.Builder()
                .addInterceptor(TurkuDynastyResponseLoggingInterceptor(settings, jacksonObjectMapper(), output::add))
                .build()
            val tokenRequest = Request.Builder().url(tokenUrl).post("client_secret=test-client-secret".toRequestBody()).build()
            val caseRequest = Request.Builder().url(casesUrl).header("Authorization", "Bearer test-token").post("{}".toRequestBody()).build()
            val errorBody = """{"error":"invalid_client","error_description":"Rejected test-client-id with test-client-secret"}"""
            val tokenBody = """{"access_token":"test-token","expires_in":3600}"""
            val caseBody = "Authorization failed for test-token"
            server.enqueue(MockResponse().setResponseCode(400).setBody(errorBody))
            server.enqueue(MockResponse().setBody(tokenBody))
            server.enqueue(MockResponse().setResponseCode(401).setBody(caseBody))

            assertThat(client.newCall(tokenRequest).execute().use { it.body?.string() }).isEqualTo(errorBody)
            assertThat(client.newCall(tokenRequest).execute().use { it.body?.string() }).isEqualTo(tokenBody)
            assertThat(client.newCall(caseRequest).execute().use { it.body?.string() }).isEqualTo(caseBody)

            assertThat(output).hasSize(3)
            assertThat(output[0]).contains("POST $tokenUrl -> HTTP 400", "invalid_client", "Rejected <redacted> with <redacted>")
            assertThat(output[1]).contains("POST $tokenUrl -> HTTP 200", "<OAuth token response omitted>")
            assertThat(output[2]).contains("POST $casesUrl -> HTTP 401", "Authorization failed for <redacted>")
            assertThat(output.joinToString()).doesNotContain("test-client-id", "test-client-secret", "test-token")
        }
    }
}
