package fi.elsapalvelu.elsa.externalintegration.sisu.hy

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationResponse
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.externalintegration.GraphQlRequest
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.impl.SisuHyClientBuilderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [SisuHyExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class SisuHyExternalIntegrationTestBase : ExternalIntegrationTestSupport() {

    @Autowired
    @Qualifier("SisuHy")
    private lateinit var sisuHyClientBuilder: GraphQLClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    protected fun executeGraphQl(endpoint: SisuHyEndpoint, hetu: String = HETU): ExternalIntegrationResponse {
        val endpointUrl = applicationProperties.getSecurity().getSisuHy().graphqlEndpointUrl!!
        val payload = GraphQlRequest(endpoint.query, mapOf("id" to hetu))
        val request = buildJsonPostRequest(endpointUrl, payload)

        logRequest(INTEGRATION, endpoint.displayName, endpointUrl, payload)

        return sisuHyClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, endpoint.displayName, it) }
        }
    }

    protected fun getQualificationsExport(): ExternalIntegrationResponse {
        val endpointUrl = applicationProperties.getSecurity().getSisuHy().tutkintoohjelmaExportUrl!!
        val request = buildGetRequest(endpointUrl)

        logRequest(INTEGRATION, "qualifications-export", endpointUrl)

        return sisuHyClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, "qualifications-export", it) }
        }
    }

    companion object {
        private const val INTEGRATION = "Sisu HY"
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(SisuHyClientBuilderImpl::class)
class SisuHyExternalIntegrationTestApplication
