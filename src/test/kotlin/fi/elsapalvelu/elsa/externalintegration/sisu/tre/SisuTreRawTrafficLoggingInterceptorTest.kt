package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SisuTreRawTrafficLoggingInterceptorTest {

    @Test
    fun `logs raw request and response bodies without credentials`() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("{\"studyrights\":[]}"))
            start()
        }
        val output = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(SisuTreRawTrafficLoggingInterceptor(output::add))
            .build()
        val requestBody = "{\"id\": \"081159-999F\"}"
        val request = Request.Builder()
            .url(server.url("/study-rights"))
            .header("Authorization", "Bearer secret-token")
            .header("Ocp-Apim-Subscription-Key", "secret-subscription-key")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().close()

            assertThat(output).containsExactly(
                "SISU TRE RAW REQUEST\nPOST ${server.url("/study-rights")}\n$requestBody",
                "SISU TRE RAW RESPONSE\nHTTP 200 OK\n{\"studyrights\":[]}"
            )
            assertThat(output.joinToString()).doesNotContain("secret-token", "secret-subscription-key")
        } finally {
            server.shutdown()
        }
    }
}

