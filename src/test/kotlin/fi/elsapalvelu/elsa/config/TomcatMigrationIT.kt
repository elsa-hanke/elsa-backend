package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.web.rest.errors.FileSizeExceptionAdvice
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import tech.jhipster.config.JHipsterProperties
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@ActiveProfiles("prod")
@SpringBootTest(
    classes = [TomcatMigrationIT.HttpTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.config.location=file:src/main/resources/config/application.yml"]
)
class TomcatMigrationIT {
    @Autowired private lateinit var context: ServletWebServerApplicationContext
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    @Test
    fun uploadAcceptsFinnishFilenameThroughRealTomcat() {
        val filename = "äö-".repeat(35) + ".pdf"
        val response = upload(listOf(filename to byteArrayOf(1, 2, 3)))
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains(filename)
    }

    @Test
    fun tomcatAcceptsFiftyPartsAndRejectsFiftyOne() {
        assertThat(upload((1..50).map { "file-$it.pdf" to byteArrayOf(1) }).statusCode()).isEqualTo(200)
        // Characterize the current limit; increasing it requires a separate upload policy decision.
        assertThat(upload((1..51).map { "file-$it.pdf" to byteArrayOf(1) }).statusCode()).isNotEqualTo(200)
    }

    @Test
    fun oversizedFileReturnsHttpErrorInsteadOfConnectionReset() {
        val response = upload(listOf("too-large.pdf" to ByteArray(21 * 1024 * 1024)))
        assertThat(response.statusCode()).isEqualTo(413)
    }

    @Test
    fun longFinnishFilenameExceedingPartHeaderLimitIsRejected() {
        val filename = "äö".repeat(120) + ".pdf"
        assertThat(filename.length).isLessThanOrEqualTo(250)
        assertThat(upload(listOf(filename to byteArrayOf(1))).statusCode()).isNotEqualTo(200)
    }

    @Test
    fun prodSessionCookieUsesSecureHttpOnlyAndSameSiteNone() {
        val response = client.send(HttpRequest.newBuilder(uri("/session")).GET().build(), HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(200)
        val cookie = response.headers().firstValue("Set-Cookie").orElseThrow()
        assertThat(cookie).contains("Secure", "HttpOnly", "SameSite=None")
    }

    private fun upload(files: List<Pair<String, ByteArray>>): HttpResponse<String> {
        val boundary = "elsa-test-boundary"
        val body = ByteArrayOutputStream()
        files.forEach { (name, content) ->
            body.write("--$boundary\r\nContent-Disposition: form-data; name=\"files\"; filename=\"$name\"\r\nContent-Type: application/pdf\r\n\r\n".toByteArray())
            body.write(content)
            body.write("\r\n".toByteArray())
        }
        body.write("--$boundary--\r\n".toByteArray())
        val request = HttpRequest.newBuilder(uri("/upload"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun uri(path: String) = URI.create("http://localhost:${requireNotNull(context.webServer).port}$path")

    // Load real production web settings, but only web components: no DB, mail or AWS clients.
    @Suppress("DEPRECATION", "Deprecation")
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(
        TomcatServletWebServerAutoConfiguration::class,
        DispatcherServletAutoConfiguration::class,
        WebMvcAutoConfiguration::class,
        ErrorMvcAutoConfiguration::class,
        MultipartAutoConfiguration::class,
        HttpEncodingAutoConfiguration::class,
        Jackson2AutoConfiguration::class
    )
    @EnableConfigurationProperties(JHipsterProperties::class)
    @Import(WebConfigurer::class, FileSizeExceptionAdvice::class)
    class HttpTestConfiguration {
        @Bean fun uploadController() = UploadController()
    }

    @RestController
    class UploadController {
        @PostMapping("/upload")
        fun upload(@RequestParam files: List<MultipartFile>): List<String> = files.map { requireNotNull(it.originalFilename) }

        @GetMapping("/session")
        fun session(request: HttpServletRequest): String = request.getSession(true).id
    }
}
