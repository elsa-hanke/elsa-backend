package fi.elsapalvelu.elsa.externalintegration.peppi.turku

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationResponse
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.externalintegration.HetuRequest
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.impl.PeppiTurkuClientBuilderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiTurkuExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class PeppiTurkuExternalIntegrationTestBase : ExternalIntegrationTestSupport() {

    @Autowired
    @Qualifier("PeppiTurku")
    private lateinit var peppiTurkuClientBuilder: OkHttpClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    protected fun postHetu(endpointPath: String, hetu: String = HETU): ExternalIntegrationResponse {
        val endpointUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/${endpointPath}"
        val payload = HetuRequest(hetu)
        val request = buildJsonPostRequest(endpointUrl, payload)

        logRequest(INTEGRATION, endpointPath, endpointUrl, payload)

        return peppiTurkuClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, endpointPath, it) }
        }
    }

    companion object {
        private const val INTEGRATION = "Peppi Turku"
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(PeppiTurkuClientBuilderImpl::class)
class PeppiTurkuExternalIntegrationTestApplication
