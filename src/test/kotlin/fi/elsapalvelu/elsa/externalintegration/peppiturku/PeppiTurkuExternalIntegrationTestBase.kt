package fi.elsapalvelu.elsa.externalintegration.peppiturku

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.impl.PeppiTurkuClientBuilderImpl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Tag("external-integration")
@SpringBootTest(classes = [PeppiTurkuExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class PeppiTurkuExternalIntegrationTestBase {

    @Autowired
    @Qualifier("PeppiTurku")
    private lateinit var peppiTurkuClientBuilder: OkHttpClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val log = LoggerFactory.getLogger(javaClass)

    protected fun postHetu(endpoint: PeppiTurkuEndpoint, hetu: String = HETU): PeppiTurkuResponse {
        val endpointUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/${endpoint.path}"
        val requestPayload = """{"hetu": "$hetu"}"""
        val request = Request.Builder()
            .url(endpointUrl)
            .post(requestPayload.toRequestBody())
            .build()

        log.info(
            "Peppi Turku {} request url={}, payload={}",
            endpoint.path,
            endpointUrl,
            requestPayload
        )
        println("Peppi Turku ${endpoint.path} request url=$endpointUrl, payload=$requestPayload")

        return peppiTurkuClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            log.info(
                "Peppi Turku {} response status={}, body={}",
                endpoint.path,
                response.code,
                responseBody
            )
            println("Peppi Turku ${endpoint.path} response status=${response.code}, body=$responseBody")

            PeppiTurkuResponse(
                statusCode = response.code,
                body = responseBody
            )
        }
    }

    protected fun assertSuccessfulResponse(response: PeppiTurkuResponse) {
        assertThat(response).isNotNull
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isNotBlank()
    }

    companion object {
        private const val HETU = "030884-227C"
    }
}

enum class PeppiTurkuEndpoint(val path: String) {
    STUDENT("student"),
    STUDY_ACCOMPLISHMENTS("study_accomplishments")
}

data class PeppiTurkuResponse(
    val statusCode: Int,
    val body: String?
)

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(PeppiTurkuClientBuilderImpl::class)
class PeppiTurkuExternalIntegrationTestApplication
