package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.peppi.turku.PeppiTurkuClientBuilderImpl
import fi.elsapalvelu.elsa.service.integration.peppi.uef.PeppiUefClientBuilderImpl
import fi.elsapalvelu.elsa.service.integration.sisu.tampere.SisuTreClientBuilderImpl
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.kayttaja.AuthenticationTokenService
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class IntegrationClientBuilderAlertConfigurationTest {

    private val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
    private val alertService = IntegrationAlertService(alertPublisherService)

    @Test
    fun `Turku client includes authentication alert interceptor`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getPeppiTurku().apply {
                apiKey = "api-key"
                basicAuthEncodedKey = "basic-auth"
            }
        }

        val client = PeppiTurkuClientBuilderImpl(properties, alertService).okHttpClient()

        assertThat(client.hasAuthenticationAlertInterceptor()).isTrue
    }

    @Test
    fun `UEF client includes authentication alert interceptor`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getPeppiUef().apiKey = "api-key"
        }

        val client = PeppiUefClientBuilderImpl(properties, alertService).okHttpClient()

        assertThat(client.hasAuthenticationAlertInterceptor()).isTrue
    }

    @Test
    fun `Tampere API client alerts on forbidden subscription key response`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getSisuTre().subscriptionKey = "subscription-key"
        }
        val authenticationTokenService = Mockito.mock(AuthenticationTokenService::class.java)
        whenever(authenticationTokenService.getCachedTokenOrRequestNew()).thenReturn("access-token")
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(403))

        val client = SisuTreClientBuilderImpl(
            authenticationTokenService,
            properties,
            alertService
        ).okHttpClient()

        try {
            client.newCall(Request.Builder().url(server.url("/study-rights")).build()).execute().close()

            assertThat(client.hasAuthenticationAlertInterceptor()).isTrue
            verify(alertPublisherService).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    private fun okhttp3.OkHttpClient.hasAuthenticationAlertInterceptor() =
        interceptors.any { it is IntegrationAuthenticationAlertInterceptor }
}
