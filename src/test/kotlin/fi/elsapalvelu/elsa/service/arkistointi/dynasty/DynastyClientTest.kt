package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import okhttp3.OkHttpClient
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit

class DynastyClientTest {
    private val grace = Duration.ofMinutes(5)
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
        return DynastyClient(settings,jacksonObjectMapper(),transport, Clock.systemDefaultZone())
    }
    private fun caseRequest() = DynastyCaseRequest(DynastyCaseModel("case-schema", "Test case", DynastyAdditionalData(fieldPrefix = "UTU")))

    private fun token() = MockResponse().setBody("""{"access_token":"test-token","expires_in":900}""")

    @ParameterizedTest
    @CsvSource(
        "oauth_token, 400, auth_failed",
        "oauth_token, 403, auth_failed",
        "oauth_token, 503, auth_temporarily_rejected",
        "create_case, 400, http_rejected",
        "create_case, 422, http_rejected",
        "create_case, 503, delivery_unconfirmed",
        "create_case, 401, auth_failed",
        "tos_document_types, 200, title_not_found",
        "tos_document_types, 429, tos_unavailable",
        "tos_phases, 400, tos_invalid",
        "tos_phases, 200, invalid_json"
    )
    fun `Dynasty failure logs HTTP error bodies and preserves job context`(
        operation: String, status: Int, reason: String
    ) {
        val logger = LoggerFactory.getLogger(DynastyCallDiagnostics::class.java) as Logger
        val events = object : ListAppender<ILoggingEvent>() {
            override fun append(event: ILoggingEvent) {
                event.prepareForDeferredProcessing()
                super.append(event)
            }
        }
        val previous = MDC.getCopyOfContextMap()
        events.start()
        logger.addAppender(events)
        try {
            MDC.put("archiveJobId", "test-job")
            MDC.put("userId", "test-approver")
            MockWebServer().use { server ->
                val requestId = UUID.randomUUID().toString()
                val tosClass = "05.06.02.04"
                val phaseId = UUID.randomUUID().toString()
                if (operation != "oauth_token") server.enqueue(token())
                if (status == 401) {
                    server.enqueue(MockResponse().setResponseCode(401).setBody("Expired token"))
                    server.enqueue(token())
                }
                if (operation == "tos_document_types") {
                    server.enqueue(MockResponse().setBody(
                        """{"fullCasegroupCode":"$tosClass","thsPhases":[{"title":"Hakeminen","taskRowId":"$phaseId"}]}"""
                    ))
                }
                val body = when (operation) {
                    "tos_document_types" -> """{"fullCasegroupCode":"$tosClass","taskRowId":"$phaseId","thsDocuments":[]}"""
                    "tos_phases" -> "{"
                    else -> "{\n  \"errors\": {\"schemaId\": [\"Invalid schema ä\"]}\n}"
                }
                server.enqueue(MockResponse().setResponseCode(status).setBody(body)
                    .addHeader("X-Request-ID", "untrusted header")
                    .addHeader("X-Correlation-ID", requestId))
                val client = client(server)
                assertThatThrownBy {
                    if (operation.startsWith("tos_")) client.resolveTosPhase(tosClass, "Hakeminen", setOf("Summary"))
                    else client.createCase(tosClass, caseRequest())
                }.isInstanceOf(DynastyFailure::class.java)
                val event = terminalFailureEvent(events.list, operation, status)
                val expectedBody = if (status !in 200..299) " responseBody=$body" else ""
                assertThat(event.level).isEqualTo(Level.ERROR)
                assertThat(event.formattedMessage)
                    .isEqualTo("Dynasty call failed [operation=$operation, reason=$reason, httpStatus=$status]$expectedBody")
                assertThat(event.throwableProxy).isNull()
                assertThat(event.mdcPropertyMap)
                    .containsEntry("archiveJobId", "test-job")
                    .containsEntry("userId", "test-approver")
                    .containsEntry("dynastyOperation", operation)
                    .containsEntry("dynastyReason", reason)
                    .containsEntry("httpStatus", status.toString())
                    .containsEntry("dynastyRequestId", requestId)
                if (reason == "invalid_json") assertThat(event.mdcPropertyMap["exceptionType"]).isNotBlank()
                assertThat(MDC.getCopyOfContextMap())
                    .doesNotContainKeys("httpStatus", "dynastyRequestId", "dynastyOperation", "dynastyReason", "exceptionType")
            }
        } finally {
            logger.detachAppender(events)
            events.stop()
            if (previous == null) MDC.clear() else MDC.setContextMap(previous)
        }
    }

    private fun terminalFailureEvent(events: List<ILoggingEvent>, operation: String, status: Int): ILoggingEvent {
        assertThat(events).hasSize(if (status == 401) 2 else 1)
        if (status == 401) {
            val firstFailure = events.first()
            assertThat(firstFailure.level).isEqualTo(Level.ERROR)
            assertThat(firstFailure.formattedMessage).isEqualTo(
                "Dynasty call failed [operation=$operation, reason=unauthorized_token_refresh, httpStatus=401] responseBody=Expired token"
            )
        }
        return events.last()
    }

    @Test
    fun `unreadable bad request body preserves the definite rejection`() {
        MockWebServer().use { server ->
            server.enqueue(token())
            server.enqueue(
                MockResponse().setResponseCode(400).setBody("response body interrupted")
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
            )

            assertThatThrownBy {
                client(server).createCase("05.06.02.04", caseRequest())
            }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.category).isEqualTo(DynastyFailureCategory.HTTP_REJECTED)
            }
        }
    }

    @ParameterizedTest
    @CsvSource("403, false", "429, true", "503, true")
    fun `token rejection keeps intent removable but retries only temporary failures`(status: Int, retryable: Boolean) {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(status).setBody("private authentication details"))
            val ledger = mock<DynastyStepLedger>()
            val execution = ArkistointiJobExecution("arkistointi-job", UUID.randomUUID().toString(), 1)
            whenever(ledger.begin(eq(execution), eq(DynastyStepKey.Case), any()))
                .thenReturn(DynastyStepLedger.Decision.Proceed)
            val c = client(server)

            assertThatThrownBy {
                DynastyStepExecutor(ledger).execute(execution, grace, DynastyStepKey.Case) {
                    c.createCase("05.06.02.04", caseRequest())
                }
            }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.category.code).isEqualTo("DYNASTY_AUTH_FAILED")
                assertThat(it.category.retryable).isEqualTo(retryable)
                assertThat(it.category.definitelyNotProcessed).isTrue()
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
            verify(ledger).abandon(execution.jobId, DynastyStepKey.Case, execution.version)
            verify(ledger, never()).complete(any(), any(), any(), anyOrNull())
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

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
            val transport = networkFailureTransport(scenario)
            enqueueMutationFailure(server, scenario)
            val ledger = mock<DynastyStepLedger>()
            val execution = ArkistointiJobExecution("arkistointi-job", UUID.randomUUID().toString(), 1)
            whenever(ledger.begin(eq(execution), eq(DynastyStepKey.Case), any()))
                .thenReturn(DynastyStepLedger.Decision.Proceed)
            val c = client(server, transport)
            assertThatThrownBy {
                DynastyStepExecutor(ledger).execute(execution, grace, DynastyStepKey.Case) {
                    c.createCase("05.06.02.04", caseRequest())
                }
            }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.category.code).isEqualTo(if (beforeRequest) "DYNASTY_UNAVAILABLE" else "DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(it.category.retryable).isEqualTo(retryable)
                assertThat(it.category.definitelyNotProcessed).isEqualTo(beforeRequest)
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
            verify(ledger, if (beforeRequest) times(1) else never())
                .abandon(execution.jobId, DynastyStepKey.Case, execution.version)
            verify(ledger, never()).complete(any(), any(), any(), anyOrNull())
            assertThat(server.requestCount).isEqualTo(if (beforeRequest) 1 else 2)
        }
    }

    private fun networkFailureTransport(scenario: String): OkHttpClient = OkHttpClient.Builder()
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

    private fun enqueueMutationFailure(server: MockWebServer, scenario: String) {
        when (scenario) {
            "response-timeout" -> server.enqueue(MockResponse().setHeadersDelay(2, TimeUnit.SECONDS).setBody("415"))
            "lost-response" -> server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
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
                    DynastyStepExecutor(ledger).execute(execution, grace, DynastyStepKey.Case) {
                        c.createCase("05.06.02.04", caseRequest())
                    }
                }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.category.code).isEqualTo("DYNASTY_UNAVAILABLE")
                    assertThat(it.category.retryable).isFalse()
                    assertThat(it.category.definitelyNotProcessed).isTrue()
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
                    assertThat(it.category.code).isEqualTo("DYNASTY_CONFIGURATION_INVALID")
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
                    assertThat(it.category.retryable).isEqualTo(!duringMutation)
                    assertThat(it.category.definitelyNotProcessed).isEqualTo(!duringMutation)
                    assertThat(it.category.code).isEqualTo(if (duringMutation) "DYNASTY_DELIVERY_UNCONFIRMED" else "DYNASTY_AUTH_UNAVAILABLE")
                }
            assertThat(server.requestCount).isEqualTo(if (duringMutation) 2 else 1)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        """{"access_token":"","expires_in":900}""",
        """{"access_token":123,"expires_in":900}""",
        """{"access_token":"test-token","expires_in":"900"}""",
        """{"access_token":"test-token","expires_in":60}""",
        """{"access_token":"test-token","expires_in":86401}"""
    ])
    fun `invalid token response sends no mutation and does not poison the cache`(body: String) {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody(body))
            val c = client(server)
            assertThatThrownBy { c.createCase("x", caseRequest()) }
                .isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.category.code).isEqualTo("DYNASTY_AUTH_FAILED")
                    assertThat(it.category.retryable).isFalse()
                    assertThat(it.category.definitelyNotProcessed).isTrue()
                }
            assertThat(server.requestCount).isEqualTo(1)
            server.enqueue(token())
            server.enqueue(MockResponse().setBody("415"))
            assertThat(c.createCase("x", caseRequest())).isEqualTo(DynastyCaseId(415))
            assertThat((1..3).map { server.takeRequest().requestUrl?.encodedPath })
                .containsExactly("/token", "/token", "/api/v1/cases")
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
                assertThat(it.category.definitelyNotProcessed).isFalse()
                assertThat(it.category.retryable).isFalse()
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
    fun `second 401 stops after one token renewal and leaves the mutation safe to retry manually`() {
        MockWebServer().use { server ->
            server.start()
            repeat(2) {
                server.enqueue(token())
                server.enqueue(MockResponse().setResponseCode(401))
            }

            assertThatThrownBy { client(server).createCase("x", caseRequest()) }
                .isInstanceOfSatisfying(DynastyFailure::class.java) {
                    assertThat(it.category.code).isEqualTo("DYNASTY_AUTH_FAILED")
                    assertThat(it.category.retryable).isFalse()
                    assertThat(it.category.definitelyNotProcessed).isTrue()
                }
            assertThat(server.requestCount).isEqualTo(4)
            assertThat((1..4).map { server.takeRequest().requestUrl?.encodedPath })
                .containsExactly("/token", "/api/v1/cases", "/token", "/api/v1/cases")
        }
    }

    @Test
    fun `invalid successful response requires reconciliation`() {
        MockWebServer().use { s ->
            s.start(); s.enqueue(token()); s.enqueue(MockResponse().setBody("not-an-id"))
            assertThatThrownBy { client(s).createCase("x",caseRequest()) }.isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.category.code).isEqualTo("DYNASTY_RESPONSE_INVALID")
                assertThat(it.category.definitelyNotProcessed).isFalse()
            }
        }
    }
}
