package fi.elsapalvelu.elsa.service.arkistointi

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

class HelsinkiSiiloServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: HelsinkiSiiloService
    private var temporaryFile: Path? = null

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val properties = ApplicationProperties()
        properties.getArkistointi().getHki().apply {
            host = server.url("/").toString().removeSuffix("/")
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

    private fun createTemporaryZip(content: String): Path {
        return Files.createTempFile("siilo-test-", ".zip").also {
            temporaryFile = it
            Files.writeString(it, content)
        }
    }
}
