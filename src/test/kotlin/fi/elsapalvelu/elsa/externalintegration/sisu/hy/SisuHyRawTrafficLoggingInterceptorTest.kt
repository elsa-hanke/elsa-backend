package fi.elsapalvelu.elsa.externalintegration.sisu.hy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SisuHyRawTrafficLoggingInterceptorTest {

    @Test
    fun `logs raw request and response with masked credentials`() {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"data\":{\"private_person_by_personal_identity_code\":null}}")
            )
            start()
        }
        val output = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(SisuHyRawTrafficLoggingInterceptor(output::add))
            .build()
        val requestBody = "{\"query\":\"query OpintotietodataSisuHy($: ID!) { ... }\",\"variables\":{\"id\":\"210281-9988\"}}"
        val request = Request.Builder()
            .url(server.url("/secure/sisu/graphql"))
            .header("X-Api-Key", "super-secret-api-key")
            .header("Authorization", "Bearer top-secret-token")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().close()

            assertThat(output).hasSize(2)
            val requestLog = output[0]
            val responseLog = output[1]

            assertThat(requestLog).contains("SISU HY RAW REQUEST")
            assertThat(requestLog).contains("POST ${server.url("/secure/sisu/graphql")}")
            assertThat(requestLog).contains("X-Api-Key: <masked>")
            assertThat(requestLog).contains("Authorization: <masked>")
            assertThat(requestLog).contains("Content-Type: application/json")
            assertThat(requestLog).contains(requestBody)

            assertThat(responseLog).contains("SISU HY RAW RESPONSE")
            assertThat(responseLog).contains("HTTP 200 OK")
            assertThat(responseLog).contains("Content-Type: application/json")
            assertThat(responseLog).contains("{\"data\":{\"private_person_by_personal_identity_code\":null}}")

            assertThat(output.joinToString()).doesNotContain("super-secret-api-key", "top-secret-token")
        } finally {
            server.shutdown()
        }
    }
}
