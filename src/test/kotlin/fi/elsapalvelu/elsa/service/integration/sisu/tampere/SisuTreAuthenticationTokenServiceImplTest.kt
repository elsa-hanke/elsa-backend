package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SisuTreAuthenticationTokenServiceImplTest {

    @Mock
    private lateinit var client: OkHttpClient

    @Mock
    private lateinit var call: Call

    @Mock
    private lateinit var alertPublisherService: AlertPublisherService

    private lateinit var service: SisuTreAuthenticationTokenServiceImpl

    @BeforeEach
    fun setUp() {
        val properties = ApplicationProperties().apply {
            getSecurity().getSisuTre().apply {
                tokenEndpointUrl = "https://login.example.test"
                tenantId = "tenant"
                scopeId = "scope"
                clientId = "client"
                clientSecret = "secret"
            }
        }
        service = SisuTreAuthenticationTokenServiceImpl(
            object : OkHttpClientBuilder {
                override fun okHttpClient() = client
            },
            jacksonObjectMapper(),
            properties,
            IntegrationAlertService(alertPublisherService)
        )
        whenever(client.newCall(any())).thenReturn(call)
    }

    @Test
    fun `malformed token response returns null`() {
        whenever(call.execute()).thenReturn(response("not-json"))

        assertThat(service.requestToken()).isNull()
        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `token response with missing required fields returns null`() {
        whenever(call.execute()).thenReturn(response("""{"access_token":null,"expires_in":null}"""))

        assertThat(service.requestToken()).isNull()
        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `valid token response returns access token`() {
        whenever(call.execute()).thenReturn(response("""{"access_token":"token-value","expires_in":60}"""))

        assertThat(service.requestToken()).isEqualTo("token-value")
        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `invalid client response publishes one sanitized alert for repeated failures`() {
        whenever(call.execute()).thenReturn(
            invalidClientResponse(),
            invalidClientResponse()
        )

        assertThat(service.requestToken()).isNull()
        assertThat(service.requestToken()).isNull()

        verify(call, times(2)).execute()
        verify(alertPublisherService, times(1)).publishAlert(
            "Tampere opintotietointegraation autentikointi epäonnistui",
            "Tampereen opintotietointegraatio ei saanut OAuth2-tokenia. " +
                "HTTP status: 400. Error: invalid_client. Error codes: 7000215. " +
                "Endpoint: https://login.example.test/tenant/oauth2/v2.0/token. " +
                "Virhe ei liity yksittäiseen henkilötunnukseen."
        )
    }

    @Test
    fun `successful token resets alert suppression`() {
        whenever(call.execute()).thenReturn(
            invalidClientResponse(),
            response("""{"access_token":"token-value","expires_in":60}"""),
            invalidClientResponse()
        )

        assertThat(service.requestToken()).isNull()
        assertThat(service.requestToken()).isEqualTo("token-value")
        assertThat(service.requestToken()).isNull()

        verify(alertPublisherService, times(2)).publishAlert(any(), any())
    }

    private fun invalidClientResponse() = response(
        body = """
            {
              "error":"invalid_client",
              "error_description":"Invalid client secret provided",
              "error_codes":[7000215],
              "timestamp":"2026-08-13 09:16:02Z",
              "trace_id":"trace-id",
              "correlation_id":"correlation-id"
            }
        """.trimIndent(),
        code = 400
    )

    private fun response(body: String, code: Int = 200) = Response.Builder()
        .request(Request.Builder().url("https://login.example.test/token").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code in 200..299) "OK" else "Error")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
