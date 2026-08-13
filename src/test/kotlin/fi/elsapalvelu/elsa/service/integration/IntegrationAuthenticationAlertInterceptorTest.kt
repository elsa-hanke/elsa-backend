package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.net.ssl.SSLHandshakeException

class IntegrationAuthenticationAlertInterceptorTest {

    @Test
    fun `authentication failures are suppressed until a successful response`() {
        val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
        val alertService = IntegrationAlertService(alertPublisherService)
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(200))
            server.enqueue(MockResponse().setResponseCode(401))
            val client = client(alertService)

            repeat(4) {
                client.newCall(Request.Builder().url(server.url("/student")).build()).execute().close()
            }

            verify(alertPublisherService, times(2)).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `person-level and transient HTTP failures do not publish an authentication alert`() {
        val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
        val alertService = IntegrationAlertService(alertPublisherService)
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(400))
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setResponseCode(500))
            val client = client(alertService)

            repeat(3) {
                client.newCall(Request.Builder().url(server.url("/student")).build()).execute().close()
            }

            verify(alertPublisherService, never()).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `TLS handshake failure publishes an alert when enabled`() {
        val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
        val interceptor = IntegrationAuthenticationAlertInterceptor(
            IntegrationAlertService(alertPublisherService),
            IntegrationAlertKey.SISU_HY_AUTHENTICATION,
            "Helsingin Sisu",
            alertOnTlsFailure = true
        )
        val chain = Mockito.mock(Interceptor.Chain::class.java)
        val request = Request.Builder().url("https://sisu.example.test/graphql").build()
        whenever(chain.request()).thenReturn(request)
        whenever(chain.proceed(request)).thenThrow(SSLHandshakeException("certificate expired"))

        assertThatThrownBy { interceptor.intercept(chain) }
            .isInstanceOf(SSLHandshakeException::class.java)

        verify(alertPublisherService).publishAlert(any(), any())
    }

    @Test
    fun `specific OAuth alert suppresses a follow-up API authentication alert`() {
        val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
        val alertService = IntegrationAlertService(alertPublisherService)
        alertService.publishOnceUntilSuccess(
            IntegrationAlertKey.SISU_TRE_OAUTH,
            "OAuth failure",
            "OAuth failure"
        )
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(401))
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    IntegrationAuthenticationAlertInterceptor(
                        alertService,
                        IntegrationAlertKey.SISU_TRE_API_AUTHENTICATION,
                        "Tampereen Sisu",
                        suppressedByAlertKey = IntegrationAlertKey.SISU_TRE_OAUTH
                    )
                )
                .build()

            client.newCall(Request.Builder().url(server.url("/study-rights")).build()).execute().close()
            alertService.markSuccessful(IntegrationAlertKey.SISU_TRE_OAUTH)
            client.newCall(Request.Builder().url(server.url("/study-rights")).build()).execute().close()

            verify(alertPublisherService, times(2)).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    private fun client(alertService: IntegrationAlertService) = OkHttpClient.Builder()
        .addInterceptor(
            IntegrationAuthenticationAlertInterceptor(
                alertService,
                IntegrationAlertKey.PEPPI_TURKU_AUTHENTICATION,
                "Turun Peppi"
            )
        )
        .build()
}
