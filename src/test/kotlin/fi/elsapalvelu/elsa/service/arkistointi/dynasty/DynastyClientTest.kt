package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DynastyClientTest {
    private fun client(server: MockWebServer, configure: DynastyProperties.() -> Unit = {}): DynastyClient {
        val p = ApplicationProperties()
        p.getArkistointi().getTurku().dynasty.apply {
            baseUrl = server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/')
            tokenUrl = server.url("/token").newBuilder().host("127.0.0.1").build().toString()
            clientId = "client"
            clientSecret = "secret"
            organization = "0"
            caseSchemaId = "case"
            documentSchemaId = "document"
            allowLoopbackHttpForTests = true
            configure()
        }
        return DynastyClient(p,jacksonObjectMapper())
    }
    private fun token() = MockResponse().setBody("""{"access_token":"test-token","expires_in":900}""")

    @ParameterizedTest
    @ValueSource(strings = ["clientId", "clientSecret", "tokenUrl", "organization"])
    fun `missing authentication setting produces a safe configuration failure without HTTP`(setting: String) {
        MockWebServer().use { server ->
            server.start()
            val client = client(server) {
                when (setting) {
                    "clientId" -> clientId = null
                    "clientSecret" -> clientSecret = null
                    "tokenUrl" -> tokenUrl = null
                    "organization" -> organization = null
                }
            }
            assertThatThrownBy { client.createCase("x", "{}") }
                .isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.code).isEqualTo("DYNASTY_CONFIGURATION_INVALID")
                    assertThat(it.cause).isNull()
                }
            assertThat(server.requestCount).isZero()
        }
    }

    @Test
    fun `token is cached and organization accompanies every request`() {
        MockWebServer().use { s ->
            s.start()
            s.enqueue(token()); s.enqueue(MockResponse().setBody("415")); s.enqueue(MockResponse().setBody("416"))
            val c = client(s)
            c.validateConfiguration()
            assertThat(c.createCase("05.06.02.04","{}")).isEqualTo("415")
            assertThat(c.createCase("05.06.02.04","{}")).isEqualTo("416")
            assertThat(s.requestCount).isEqualTo(3)
            val auth = s.takeRequest()
            assertThat(auth.getHeader("Organization")).isEqualTo("0")
            assertThat(auth.body.readUtf8()).contains("grant_type=client_credentials","client_secret=secret")
            val request = s.takeRequest()
            assertThat(request.path).isEqualTo("/api/v1/cases?tosClass=05.06.02.04")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token")
        }
    }

    @Test
    fun `503 is ambiguous and is never transparently retried`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setResponseCode(503).setBody("private details"))
            assertThatThrownBy { client(s).createCase("x","{}") }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.definitelyNotProcessed).isFalse()
                assertThat(it.retryable).isFalse()
                assertThat(it.message).doesNotContain("private details")
            }
            assertThat(s.requestCount).isEqualTo(2)
        }
    }

    @Test
    fun `401 renews token once before repeating rejected request`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setResponseCode(401)); s.enqueue(token()); s.enqueue(MockResponse().setBody("415"))
            assertThat(client(s).createCase("x","{}")).isEqualTo("415")
            assertThat(s.requestCount).isEqualTo(4)
        }
    }

    @Test
    fun `invalid successful response requires reconciliation`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setBody("not-an-id"))
            assertThatThrownBy { client(s).createCase("x","{}") }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.code).isEqualTo("DYNASTY_RESPONSE_INVALID")
                assertThat(it.definitelyNotProcessed).isFalse()
            }
        }
    }
}
