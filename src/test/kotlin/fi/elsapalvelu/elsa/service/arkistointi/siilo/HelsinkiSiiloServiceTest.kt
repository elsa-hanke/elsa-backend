package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HelsinkiSiiloServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: HelsinkiSiiloService
    private var temporaryFile: Path? = null
    private lateinit var properties: ApplicationProperties

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        properties = ApplicationProperties()
        properties.getArkistointi().getHki().apply {
            host = server.url("/").newBuilder().host("127.0.0.1").build().toString().removeSuffix("/")
            asyncAllowLoopbackHttpForTests = true
            apiKey = "test-api-key"
            metadata = ApplicationProperties.Arkistointi.Metadata().apply {
                cases = mapOf(
                    CaseType.VALMISTUMINEN.value to ApplicationProperties.Arkistointi.Case().apply {
                        siiloKoodi = "siilo-123"
                    }
                )
            }
        }
        service = HelsinkiSiiloService(properties)
    }

    @AfterEach
    fun tearDown() {
        temporaryFile?.let(Files::deleteIfExists)
        server.shutdown()
    }

    @Test
    fun `laheta posts archive zip as authenticated multipart request and deletes temporary file`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("archived"))
        val zipFile = createTemporaryZip("siilo-package")

        service.laheta(zipFile.toString(), CaseType.VALMISTUMINEN)

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/unisign/elsa/archive/siilo-123")
        assertThat(request.getHeader("X-Api-Key")).isEqualTo("test-api-key")
        assertThat(request.getHeader("Content-Type")).startsWith("multipart/form-data;")
        assertThat(request.body.readUtf8())
            .contains("name=\"file\"")
            .contains("filename=\"${zipFile.fileName}\"")
            .contains("siilo-package")
        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta reports non-successful response and deletes temporary file`() {
        server.enqueue(MockResponse().setResponseCode(503).setStatus("HTTP/1.1 503 Service Unavailable").setBody("maintenance"))
        val zipFile = createTemporaryZip("failed-package")

        assertThatThrownBy {
            service.laheta(zipFile.toString(), CaseType.VALMISTUMINEN)
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("HTTP 503")
            .hasMessageContaining("maintenance")

        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta propagates connection failure and deletes temporary file`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val zipFile = createTemporaryZip("disconnected-package")

        assertThatThrownBy {
            service.laheta(zipFile.toString(), CaseType.VALMISTUMINEN)
        }.isInstanceOf(java.io.IOException::class.java)

        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta fails before making request when archive file does not exist`() {
        val missingFile = Files.createTempFile("missing-siilo-", ".zip")
        Files.delete(missingFile)

        assertThatThrownBy {
            service.laheta(missingFile.toString(), CaseType.VALMISTUMINEN)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Arkistointitiedostoa ei löydy")

        assertThat(server.requestCount).isZero()
    }

    @ParameterizedTest
    @ValueSource(ints = [200, 400, 401, 403, 408, 409, 429, 500, 503])
    fun `async uses the legacy request protocol with safe HTTP classification`(status: Int) {
        server.enqueue(MockResponse().setResponseCode(status).setBody("sensitive remote details"))
        val file = createTemporaryZip("saved-pdf-bytes")
        if (status == 200) service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN)
        else assertThatThrownBy { service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN) }
            .isInstanceOfSatisfying(SiiloTransferException::class.java) {
                assertThat(it.httpStatus).isEqualTo(status)
                assertThat(it.retryable).isEqualTo(status in setOf(408, 429, 500, 503))
                assertThat(it.message).doesNotContain("sensitive", "remote details", file.fileName.toString())
                assertThat(it.cause).isNull()
            }
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/unisign/elsa/archive/siilo-123")
        assertThat(request.getHeader("X-Api-Key")).isEqualTo("test-api-key")
        assertThat(request.body.readUtf8()).contains("name=\"file\"", "saved-pdf-bytes")
        // Async adapter owns the directory and cleanup, unlike the legacy method.
        assertThat(file).exists()
    }

    @Test
    fun `async accepts a response after the legacy fifteen second read timeout`() {
        properties.getArkistointi().getHki().asyncTransferTimeout = Duration.ofSeconds(40)
        server.enqueue(MockResponse().setResponseCode(200).setHeadersDelay(16, TimeUnit.SECONDS))
        val file = createTemporaryZip("slow-archive")

        service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN)

        assertThat(server.requestCount).isEqualTo(1)
        assertThat(file).exists()
    }

    @Test
    fun `async total timeout cancels the exchange without changing sync timeout policy`() {
        properties.getArkistointi().getHki().asyncTransferTimeout = Duration.ofMillis(100)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val file = createTemporaryZip("timeout")
        val start = System.nanoTime()
        assertThatThrownBy { service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN) }
            .isInstanceOfSatisfying(SiiloTransferException::class.java) {
                assertThat(it.code).isEqualTo("SIILO_DELIVERY_UNCONFIRMED")
                assertThat(it.category).isEqualTo("TIMEOUT")
                assertThat(it.retryable).isTrue()
                assertThat(it.cause).isNull()
            }
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3))
        server.enqueue(MockResponse().setResponseCode(200).setHeadersDelay(300, TimeUnit.MILLISECONDS))
        service.laheta(file.toString(), CaseType.VALMISTUMINEN)
        assertThat(file).doesNotExist()
    }

    @ParameterizedTest
    @ValueSource(strings = ["host", "key", "code", "timeout"])
    fun `invalid async configuration is permanent and makes no HTTP request`(missing: String) {
        properties.getArkistointi().getHki().apply {
            when (missing) {
                "host" -> host = "invalid host"
                "key" -> apiKey = null
                "code" -> metadata!!.getCaseMetadata(CaseType.VALMISTUMINEN)!!.siiloKoodi = null
                "timeout" -> asyncTransferTimeout = Duration.ZERO
            }
        }
        val file = createTemporaryZip("configuration")
        assertThatThrownBy { service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN) }
            .isInstanceOfSatisfying(SiiloTransferException::class.java) {
                assertThat(it.code).isEqualTo("SIILO_CONFIGURATION_INVALID")
                assertThat(it.retryable).isFalse()
                assertThat(it.cause).isNull()
            }
        assertThat(server.requestCount).isZero()
    }

    @ParameterizedTest
    @ValueSource(strings = ["default", "remote", "hostname", "lookalike"])
    fun `async rejects HTTP outside explicit literal loopback tests before sending`(scenario: String) {
        val settings = properties.getArkistointi().getHki()
        when (scenario) {
            "default" -> settings.asyncAllowLoopbackHttpForTests = false
            "remote" -> settings.host = "http://192.0.2.1"
            "hostname" -> settings.host = "http://localhost"
            "lookalike" -> settings.host = "http://127.0.0.1.example.invalid"
        }
        val file = createTemporaryZip("must-not-send")
        assertThatThrownBy { service.lahetaAsync(file.toString(), CaseType.VALMISTUMINEN) }
            .isInstanceOfSatisfying(SiiloTransferException::class.java) {
                assertThat(it.code).isEqualTo("SIILO_CONFIGURATION_INVALID")
                assertThat(it.retryable).isFalse()
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain(settings.host!!, "test-api-key")
            }
        assertThat(server.requestCount).isZero()
    }

    @Test
    fun `loopback HTTP exception is disabled by default`() {
        assertThat(ApplicationProperties().getArkistointi().getHki().asyncAllowLoopbackHttpForTests).isFalse()
    }

    @Test
    fun `network failures retain only safe diagnostic categories`() {
        val failures = mapOf(
            java.net.SocketTimeoutException("private hostname") to "TIMEOUT",
            java.net.UnknownHostException("private hostname") to "DNS",
            java.net.ConnectException("private address") to "CONNECT",
            javax.net.ssl.SSLHandshakeException("private certificate") to "TLS",
            java.io.IOException("private filename") to "IO"
        )
        failures.forEach { (cause, category) ->
            val error = SiiloTransferException("SIILO_DELIVERY_UNCONFIRMED", true, category = siiloIoCategory(cause))
            assertThat(error.category).isEqualTo(category)
            assertThat(error.message).contains(category).doesNotContain("private")
            assertThat(error.cause).isNull()
        }
    }

    private fun createTemporaryZip(content: String): Path {
        return Files.createTempFile("siilo-test-", ".zip").also {
            temporaryFile = it
            Files.writeString(it, content)
        }
    }
}
