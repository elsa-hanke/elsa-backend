package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationResponse
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.externalintegration.IdRequest
import fi.elsapalvelu.elsa.service.AuthenticationTokenService
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.impl.AuthenticationTokenClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreAuthenticationTokenServiceImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreClientBuilderImpl
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [SisuTreExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
abstract class SisuTreExternalIntegrationTestBase : ExternalIntegrationTestSupport() {

    @Autowired
    @Qualifier("SisuTre")
    private lateinit var sisuTreClientBuilder: OkHttpClientBuilder

    @Autowired
    private lateinit var authenticationTokenService: AuthenticationTokenService

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    protected fun requestAccessToken(): String? {
        logRequest(INTEGRATION, "oauth-token", tokenEndpointUrl())
        return authenticationTokenService.requestToken().also {
            println("$INTEGRATION oauth-token response accessTokenPresent=${!it.isNullOrBlank()}")
        }
    }

    protected fun postId(endpointPath: String, hetu: String = HETU): ExternalIntegrationResponse {
        val endpointUrl = "${applicationProperties.getSecurity().getSisuTre().endpointUrl!!}/${endpointPath}"
        val payload = IdRequest(hetu)
        val request = buildJsonPostRequest(endpointUrl, payload)

        logRequest(INTEGRATION, endpointPath, endpointUrl, payload)

        return sisuTreClientBuilder.okHttpClient().newCall(request).execute().use { response ->
            ExternalIntegrationResponse(
                statusCode = response.code,
                body = response.body?.string()
            ).also { logResponse(INTEGRATION, endpointPath, it) }
        }
    }

    protected fun assertSuccessfulToken(accessToken: String?) {
        assertThat(accessToken).isNotBlank()
    }

    private fun tokenEndpointUrl(): String {
        val sisuTre = applicationProperties.getSecurity().getSisuTre()
        return "${sisuTre.tokenEndpointUrl}/${sisuTre.tenantId}/oauth2/v2.0/token"
    }

    companion object {
        private const val INTEGRATION = "Sisu TRE"
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    AuthenticationTokenClientBuilderImpl::class,
    SisuTreAuthenticationTokenServiceImpl::class,
    SisuTreClientBuilderImpl::class
)
class SisuTreExternalIntegrationTestApplication {
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
