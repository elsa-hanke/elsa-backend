package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.impl.PeppiTurkuClientBuilderImpl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Tag("external-integration")
@SpringBootTest(classes = [PeppiTurkuExternalIntegrationIT.ExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class PeppiTurkuExternalIntegrationIT {

    @Autowired
    @Qualifier("PeppiTurku")
    private lateinit var peppiTurkuClientBuilder: OkHttpClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val endpointUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/study_accomplishments"
        val requestBody = """{"hetu": "$HETU"}""".toRequestBody()
        val request = Request.Builder()
            .url(endpointUrl)
            .post(requestBody)
            .build()

        peppiTurkuClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            log.info(
                "Peppi Turku study_accomplishments response status={}, body={}",
                response.code,
                responseBody
            )

            assertThat(response).isNotNull
            assertThat(response.code).isEqualTo(200)
            assertThat(responseBody).isNotNull
        }
    }

    companion object {
        private const val HETU = "030884-227C"
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(ApplicationProperties::class)
    @Import(PeppiTurkuClientBuilderImpl::class)
    class ExternalIntegrationTestApplication
}
