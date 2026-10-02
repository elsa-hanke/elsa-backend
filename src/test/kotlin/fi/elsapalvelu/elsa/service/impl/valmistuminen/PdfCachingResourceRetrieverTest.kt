package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class PdfCachingResourceRetrieverTest {

    private lateinit var server: HttpServer
    private val requests = AtomicInteger()
    private val retriever = PdfCachingResourceRetriever()

    private val content = ".fa { font-family: 'Font Awesome 5 Free'; }".toByteArray()

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/all.css") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.createContext("/missing.css") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.createContext("/huge.css") { exchange ->
            requests.incrementAndGet()
            val chunk = ByteArray(1024 * 1024)
            exchange.sendResponseHeaders(200, OVER_MAX_SIZE.toLong())
            exchange.responseBody.use { stream ->
                repeat(OVER_MAX_SIZE / chunk.size) { stream.write(chunk) }
            }
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    private fun url(path: String) =
        URI("http://127.0.0.1:${server.address.port}$path").toURL()

    @Test
    fun `reads the vendored stylesheet from the classpath without using the network`() {
        val result = retriever.getByteArrayByUrl(URI("$FONTAWESOME/css/all.css").toURL())

        assertThat(result).isEqualTo(fromClasspath("vendor/fontawesome/5.15.4/css/all.css"))
        assertThat(requests.get()).isZero()
    }

    @Test
    fun `reads the vendored icon fonts from the classpath without using the network`() {
        listOf("fa-brands-400.woff2", "fa-regular-400.woff2", "fa-solid-900.woff2").forEach { name ->
            val result = retriever.getByteArrayByUrl(URI("$FONTAWESOME/webfonts/$name").toURL())

            assertThat(result)
                .describedAs(name)
                .isEqualTo(fromClasspath("vendor/fontawesome/5.15.4/webfonts/$name"))
        }

        assertThat(requests.get()).isZero()
    }

    @Test
    fun `query parameters and fragments do not prevent the local lookup`() {
        val result = retriever.getByteArrayByUrl(
            URI("$FONTAWESOME/webfonts/fa-solid-900.woff2?v=5.15.4#iefix").toURL()
        )

        assertThat(result)
            .isEqualTo(fromClasspath("vendor/fontawesome/5.15.4/webfonts/fa-solid-900.woff2"))
    }

    @Test
    fun `the vendored directory cannot be escaped by path traversal`() {
        assertThat(retriever.localPath("$FONTAWESOME/../../../../etc/passwd")).isNull()
        assertThat(retriever.localPath("$FONTAWESOME/css/../../../../etc/passwd")).isNull()
        assertThat(retriever.localPath("$FONTAWESOME/")).isNull()
    }

    @Test
    fun `a known address maps to the vendored directory`() {
        assertThat(retriever.localPath("$FONTAWESOME/css/all.css"))
            .isEqualTo("vendor/fontawesome/5.15.4/css/all.css")
        assertThat(retriever.localPath("https://example.invalid/style.css")).isNull()
    }

    @Test
    fun `a resource that is not vendored is downloaded as a last resort`() {
        assertThat(retriever.getByteArrayByUrl(url("/all.css"))).isEqualTo(content)
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `a remote resource is downloaded only once however many times it is requested`() {
        repeat(10) {
            assertThat(retriever.getByteArrayByUrl(url("/all.css"))).isEqualTo(content)
        }

        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `the same content is returned as an input stream too`() {
        val fromStream = retriever.getInputStreamByUrl(url("/all.css"))?.use { it.readBytes() }

        assertThat(fromStream).isEqualTo(content)
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `a failed download returns null and is not retried`() {
        repeat(10) {
            assertThat(retriever.getByteArrayByUrl(url("/missing.css"))).isNull()
        }

        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `clearing the cache forces a new download`() {
        retriever.getByteArrayByUrl(url("/all.css"))
        retriever.clearCache()
        retriever.getByteArrayByUrl(url("/all.css"))

        assertThat(requests.get()).isEqualTo(2)
    }

    @Test
    fun `an oversized remote resource is rejected`() {
        assertThat(retriever.getByteArrayByUrl(url("/huge.css"))).isNull()
    }

    private fun fromClasspath(path: String): ByteArray =
        ClassPathResource(path).inputStream.use { it.readBytes() }

    private companion object {
        const val FONTAWESOME = "https://use.fontawesome.com/releases/v5.15.4"
        const val OVER_MAX_SIZE = 11 * 1024 * 1024
    }
}

