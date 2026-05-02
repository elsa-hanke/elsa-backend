package fi.elsapalvelu.elsa.externalintegration.peppi.uef

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationResponse
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.externalintegration.HetuRequest
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.impl.PeppiUefClientBuilderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiUefExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class PeppiUefExternalIntegrationTestBase : ExternalIntegrationTestSupport() {

    @Autowired
    @Qualifier("PeppiUef")
    private lateinit var peppiUefClientBuilder: OkHttpClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    protected fun postHetu(endpoint: PeppiUefEndpoint, hetu: String = HETU): ExternalIntegrationResponse {
        val endpointUrl = "${applicationProperties.getSecurity().getPeppiUef().endpointUrl!!}/${endpoint.path}"
        val payload = HetuRequest(hetu)
        val request = buildJsonPostRequest(endpointUrl, payload)

        logRequest(INTEGRATION, endpoint.path, endpointUrl, payload)

        return peppiUefClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, endpoint.path, it) }
        }
    }

    companion object {
        private const val INTEGRATION = "Peppi UEF"
    }
}

enum class PeppiUefEndpoint(val path: String) {
    STUDENT("elsa-1"),
    STUDY_ACCOMPLISHMENTS("elsa-2")
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(PeppiUefClientBuilderImpl::class)
class PeppiUefExternalIntegrationTestApplication
