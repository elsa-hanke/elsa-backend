package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationResponse
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.externalintegration.GraphQlRequest
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.impl.PeppiOuluClientBuilderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiOuluExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class PeppiOuluExternalIntegrationTestBase : ExternalIntegrationTestSupport() {

    @Autowired
    @Qualifier("PeppiOulu")
    private lateinit var peppiOuluClientBuilder: GraphQLClientBuilder

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    protected fun executeGraphQl(endpoint: PeppiOuluEndpoint, hetu: String = HETU): ExternalIntegrationResponse {
        val endpointUrl = applicationProperties.getSecurity().getPeppiOulu().graphqlEndpointUrl!!
        val payload = GraphQlRequest(endpoint.query, mapOf("id" to hetu))
        val request = buildJsonPostRequest(endpointUrl, payload)

        logRequest(INTEGRATION, endpoint.displayName, endpointUrl, payload)

        return peppiOuluClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, endpoint.displayName, it) }
        }
    }

    companion object {
        private const val INTEGRATION = "Peppi Oulu"
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(PeppiOuluClientBuilderImpl::class)
class PeppiOuluExternalIntegrationTestApplication
