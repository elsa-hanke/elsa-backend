package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.UnknownHostException

class DynastyTrafficLoggingInterceptorTest {
    private val settings = DynastyProperties().apply {
        clientId = "test-client-id"
        clientSecret = "test-client-secret"
    }

    @Test
    fun `logs OAuth error status and body without exposing credentials or consuming response`() {
        MockWebServer().use { server ->
            server.start()
            val body = """{"error":"invalid_client","error_description":"Unknown test-client-id test-client-secret","access_token":"unexpected-token"}"""
            server.enqueue(MockResponse().setResponseCode(400).setBody(body))
            val logs = mutableListOf<String>()
            val client = loggingClient(logs)
            val request = Request.Builder().url(server.url("/connect/token"))
                .post("client_id=test-client-id&client_secret=test-client-secret".toRequestBody()).build()

            client.newCall(request).execute().use { response ->
                assertThat(response.body?.string()).isEqualTo(body)
            }

            assertThat(logs).hasSize(3)
            assertThat(logs[0]).isEqualTo("Dynasty request: POST ${server.url("/connect/token")}")
            assertThat(logs[1]).contains("HTTP 400", "protocol=http/1.1")
            assertThat(logs[2]).contains("invalid_client", "Unknown [REDACTED] [REDACTED]")
            assertThat(logs.joinToString()).doesNotContain("test-client-id", "test-client-secret", "unexpected-token")
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    @Test
    fun `large error body is bounded without truncating response received by client`() {
        MockWebServer().use { server ->
            server.start()
            val body = "Remote error " + "x".repeat(20000)
            server.enqueue(MockResponse().setResponseCode(502).setBody(body))
            val logs = mutableListOf<String>()
            val request = Request.Builder().url(server.url("/token")).build()

            loggingClient(logs).newCall(request).execute().use { response ->
                assertThat(response.body?.string()).isEqualTo(body)
            }

            assertThat(logs.last()).startsWith("Dynasty error response body: Remote error ").endsWith(" [truncated]")
            assertThat(logs.last().length).isLessThan(17000)
        }
    }

    @Test
    fun `successful token response and mutation payload are not logged`() {
        MockWebServer().use { server ->
            server.start()
            val body = """{"access_token":"successful-token","expires_in":900}"""
            server.enqueue(MockResponse().setBody(body))
            val logs = mutableListOf<String>()
            val request = Request.Builder().url(server.url("/token"))
                .post("private document payload".toRequestBody()).build()

            loggingClient(logs).newCall(request).execute().use { response ->
                assertThat(response.body?.string()).isEqualTo(body)
            }

            assertThat(logs).hasSize(2)
            assertThat(logs[1]).contains("HTTP 200")
            assertThat(logs.joinToString()).doesNotContain("successful-token", "private document payload")
        }
    }

    @Test
    fun `mutation error redacts bearer token and escapes multiline body`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(403).setBody("Rejected Bearer mutation-token\nAccess denied"))
            val logs = mutableListOf<String>()
            val request = Request.Builder().url(server.url("/api/v1/cases?tosClass=05.06.02.04"))
                .header("Authorization", "Bearer mutation-token").post(ByteArray(0).toRequestBody()).build()

            loggingClient(logs).newCall(request).execute().close()

            assertThat(logs[0]).contains("/api/v1/cases?tosClass=05.06.02.04")
            assertThat(logs[1]).contains("HTTP 403")
            assertThat(logs[2]).contains("Rejected Bearer [REDACTED]\\nAccess denied")
            assertThat(logs.joinToString()).doesNotContain("mutation-token")
        }
    }

    @Test
    fun `logs transport exception and underlying cause and rethrows original failure`() {
        val logs = mutableListOf<String>()
        val failure = IOException("Connection failed test-client-secret", UnknownHostException("asta.utu.fi"))
        val client = loggingClient(logs, Interceptor { throw failure })
        val request = Request.Builder().url("https://asta.utu.fi/connect/token")
            .post(ByteArray(0).toRequestBody()).build()

        assertThatThrownBy { client.newCall(request).execute() }.isSameAs(failure)

        assertThat(logs).hasSize(2)
        assertThat(logs[1]).contains(
            "POST https://asta.utu.fi/connect/token", "java.io.IOException: Connection failed [REDACTED]",
            "java.net.UnknownHostException: asta.utu.fi"
        ).doesNotContain("test-client-secret")
    }

    private fun loggingClient(logs: MutableList<String>, failure: Interceptor? = null): OkHttpClient =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .addInterceptor(DynastyTrafficLoggingInterceptor(settings, logs::add))
            .apply { if (failure != null) addInterceptor(failure) }
            .build()
}
