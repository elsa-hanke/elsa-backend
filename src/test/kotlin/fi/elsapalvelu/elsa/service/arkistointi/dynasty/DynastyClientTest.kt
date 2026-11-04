package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import okhttp3.OkHttpClient
import org.mockito.kotlin.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

class DynastyClientTest {
    private fun client(server: MockWebServer, transport: OkHttpClient? = null, configure: DynastyProperties.() -> Unit = {}): DynastyClient {
        val settings = DynastyProperties().apply {
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
        return DynastyClient(settings,jacksonObjectMapper(),transport)
    }
    private fun caseRequest() = DynastyCaseRequest(DynastyCaseModel("case-schema", "Test case", DynastyAdditionalData()))

    private fun token() = MockResponse().setBody("""{"access_token":"test-token","expires_in":900}""")

    @ParameterizedTest
    @ValueSource(strings = ["/", "/dynasty", "/dynasty/"])
    fun `case URL preserves base path and encodes tosClass as a single query value`(basePath: String) {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(token())
            server.enqueue(MockResponse().setBody("415"))
            val c = client(server) { baseUrl = requireNotNull(baseUrl).trimEnd('/') + basePath }
            val tosClass = "05.06 &other=value+plus#fragment?/% ä"

            assertThat(c.createCase(tosClass, caseRequest())).isEqualTo(DynastyCaseId(415))

            server.takeRequest() // OAuth request uses its separately configured URL.
            val url = requireNotNull(server.takeRequest().requestUrl)
            assertThat(url.encodedPath).isEqualTo(basePath.trimEnd('/') + "/api/v1/cases")
            assertThat(url.querySize).isEqualTo(1)
            assertThat(url.queryParameterNames).containsExactly("tosClass")
            assertThat(url.queryParameter("tosClass")).isEqualTo(tosClass)
            assertThat(url.fragment).isNull()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["connect-timeout", "call-timeout", "tls", "response-timeout", "lost-response"])
    fun `network failure only abandons intent when mutation has not started`(scenario: String) {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(token())
            val beforeRequest = scenario in setOf("connect-timeout", "call-timeout", "tls")
            val retryable = beforeRequest && scenario != "tls"
            val transport = OkHttpClient.Builder()
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                .callTimeout(500, TimeUnit.MILLISECONDS)
                .addInterceptor { chain ->
                    if (chain.request().url.encodedPath == "/token") return@addInterceptor chain.proceed(chain.request())
                    when (scenario) {
                        // Deterministic connect failure, before OkHttp can write any headers.
                        "connect-timeout" -> throw SocketTimeoutException("private connection details")
                        "call-timeout" -> {
                            while (!chain.call().isCanceled()) Thread.sleep(5)
                            throw IOException("private cancelled request")
                        }
                        "tls" -> throw javax.net.ssl.SSLHandshakeException("private certificate")
                        else -> chain.proceed(chain.request())
                    }
                }.build()
            if (scenario == "response-timeout") {
                server.enqueue(MockResponse().setHeadersDelay(2, TimeUnit.SECONDS).setBody("415"))
            }
            if (scenario == "lost-response") {
                server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
            }
            val ledger = mock<DynastyStepLedger>()
            val execution = ArkistointiJobExecution("arkistointi-job", UUID.randomUUID().toString(), 1)
            whenever(ledger.begin(eq(execution), eq(DynastyStepKey.Case), any()))
                .thenReturn(DynastyStepLedger.Decision.Proceed)
            val c = client(server, transport)
            assertThatThrownBy {
                DynastyStepExecutor(ledger).execute(execution, DynastyStepKey.Case, c.inProgressGrace) {
                    c.createCase("05.06.02.04", caseRequest())
                }
            }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.code).isEqualTo(if (beforeRequest) "DYNASTY_UNAVAILABLE" else "DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(it.retryable).isEqualTo(retryable)
                assertThat(it.definitelyNotProcessed).isEqualTo(beforeRequest)
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
            verify(ledger, if (beforeRequest) times(1) else never())
                .abandon(execution.jobId, DynastyStepKey.Case, execution.version)
            verify(ledger, never()).complete(any(), any(), any(), anyOrNull())
            assertThat(server.requestCount).isEqualTo(if (beforeRequest) 1 else 2)
        }
    }

    @Test
    fun `untrusted mutation TLS certificate abandons intent without automatic retry`() {
        MockWebServer().use { auth ->
            MockWebServer().use { remote ->
                val certificate = okhttp3.tls.HeldCertificate.Builder().commonName("localhost").build()
                val certificates = okhttp3.tls.HandshakeCertificates.Builder().heldCertificate(certificate).build()
                remote.useHttps(certificates.sslSocketFactory(), false)
                remote.start()
                auth.start()
                auth.enqueue(token())
                val c = client(auth) { baseUrl = remote.url("/").toString().trimEnd('/') }
                val ledger = mock<DynastyStepLedger>()
                val execution = ArkistointiJobExecution("arkistointi-job", UUID.randomUUID().toString(), 1)
                whenever(ledger.begin(eq(execution), eq(DynastyStepKey.Case), any()))
                    .thenReturn(DynastyStepLedger.Decision.Proceed)
                assertThatThrownBy {
                    DynastyStepExecutor(ledger).execute(execution, DynastyStepKey.Case, c.inProgressGrace) {
                        c.createCase("05.06.02.04", caseRequest())
                    }
                }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.code).isEqualTo("DYNASTY_UNAVAILABLE")
                    assertThat(it.retryable).isFalse()
                    assertThat(it.definitelyNotProcessed).isTrue()
                    assertThat(it.cause).isNull()
                }
                verify(ledger).abandon(execution.jobId, DynastyStepKey.Case, execution.version)
                assertThat(remote.requestCount).isZero()
            }
        }
    }

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
            assertThatThrownBy { client.createCase("x", caseRequest()) }
                .isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.code).isEqualTo("DYNASTY_CONFIGURATION_INVALID")
                    assertThat(it.cause).isNull()
                }
            assertThat(server.requestCount).isZero()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `lost token response can retry but lost mutation response requires reconciliation`(duringMutation: Boolean) {
        MockWebServer().use { server ->
            server.start()
            if (duringMutation) server.enqueue(token())
            server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
            assertThatThrownBy { client(server).createCase("x", caseRequest()) }
                .isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.retryable).isEqualTo(!duringMutation)
                    assertThat(it.definitelyNotProcessed).isEqualTo(!duringMutation)
                    assertThat(it.code).isEqualTo(if (duringMutation) "DYNASTY_DELIVERY_UNCONFIRMED" else "DYNASTY_AUTH_UNAVAILABLE")
                }
            assertThat(server.requestCount).isEqualTo(if (duringMutation) 2 else 1)
        }
    }

    @Test
    fun `token is cached and organization accompanies every request`() {
        MockWebServer().use { s ->
            s.start()
            s.enqueue(token()); s.enqueue(MockResponse().setBody("415")); s.enqueue(MockResponse().setBody("416"))
            val c = client(s)
            c.validateConfiguration()
            assertThat(c.createCase("05.06.02.04",caseRequest())).isEqualTo(DynastyCaseId(415))
            assertThat(c.createCase("05.06.02.04",caseRequest())).isEqualTo(DynastyCaseId(416))
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
    fun `destinations keep independent credentials tokens and organization headers`() {
        MockWebServer().use { first ->
            MockWebServer().use { second ->
                first.start()
                second.start()
                first.enqueue(MockResponse().setBody("""{"access_token":"first-token","expires_in":900}"""))
                second.enqueue(MockResponse().setBody("""{"access_token":"second-token","expires_in":900}"""))
                listOf(first, second).forEach { server ->
                    server.enqueue(MockResponse().setBody("415"))
                    server.enqueue(MockResponse().setBody("416"))
                }
                val a = client(first) { organization = "first"; clientSecret = "first-secret" }
                val b = client(second) { organization = "second"; clientSecret = "second-secret" }
                a.createCase("x", caseRequest())
                b.createCase("x", caseRequest())
                a.createCase("x", caseRequest())
                b.createCase("x", caseRequest())
                listOf(first to "first", second to "second").forEach { (server, destination) ->
                    val auth = server.takeRequest()
                    assertThat(auth.getHeader("Organization")).isEqualTo(destination)
                    assertThat(auth.body.readUtf8()).contains("client_secret=$destination-secret")
                    repeat(2) {
                        val request = server.takeRequest()
                        assertThat(request.getHeader("Organization")).isEqualTo(destination)
                        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $destination-token")
                    }
                    assertThat(server.requestCount).isEqualTo(3)
                }
            }
        }
    }

    @Test
    fun `503 is ambiguous and is never transparently retried`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setResponseCode(503).setBody("private details"))
            assertThatThrownBy { client(s).createCase("x",caseRequest()) }.isInstanceOfSatisfying(DynastyFailure::class.java) {
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
            assertThat(client(s).createCase("x",caseRequest())).isEqualTo(DynastyCaseId(415))
            assertThat(s.requestCount).isEqualTo(4)
        }
    }

    @Test
    fun `invalid successful response requires reconciliation`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setBody("not-an-id"))
            assertThatThrownBy { client(s).createCase("x",caseRequest()) }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.code).isEqualTo("DYNASTY_RESPONSE_INVALID")
                assertThat(it.definitelyNotProcessed).isFalse()
            }
        }
    }
}
