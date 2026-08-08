package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
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
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SisuTreAuthenticationTokenServiceImplTest {

    @Mock
    private lateinit var client: OkHttpClient

    @Mock
    private lateinit var call: Call

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
            properties
        )
        whenever(client.newCall(any())).thenReturn(call)
    }

    @Test
    fun `malformed token response returns null`() {
        whenever(call.execute()).thenReturn(response("not-json"))

        assertThat(service.requestToken()).isNull()
    }

    @Test
    fun `token response with missing required fields returns null`() {
        whenever(call.execute()).thenReturn(response("""{"access_token":null,"expires_in":null}"""))

        assertThat(service.requestToken()).isNull()
    }

    @Test
    fun `valid token response returns access token`() {
        whenever(call.execute()).thenReturn(response("""{"access_token":"token-value","expires_in":60}"""))

        assertThat(service.requestToken()).isEqualTo("token-value")
    }

    private fun response(body: String) = Response.Builder()
        .request(Request.Builder().url("https://login.example.test/token").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
