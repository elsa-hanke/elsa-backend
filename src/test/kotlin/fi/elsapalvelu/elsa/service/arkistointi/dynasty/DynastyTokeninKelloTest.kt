package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.time.Clock
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DynastyTokeninKelloTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `token uusitaan turvamarginaalin rajalla mutta ei ennen sita`(aika: KellonTestitapaus) {
        MockWebServer().use { palvelin ->
            palvelin.start()
            val kello = mock<Clock>()
            whenever(kello.instant()).thenReturn(aika.aikaleima())
            val asetukset = DynastyProperties().apply {
                baseUrl = palvelin.url("/").toString()
                tokenUrl = palvelin.url("/token").toString()
                clientId = "asiakas"
                clientSecret = "salaisuus"
                organization = "testi"
                scope = "testi"
                allowLoopbackHttpForTests = true
            }
            var tokenienMaara = 0
            palvelin.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = if (request.path == "/token") {
                    tokenienMaara++
                    val token = if (tokenienMaara == 1) "ensimmainen" else "toinen"
                    MockResponse().setBody("""{"access_token":"$token","expires_in":120}""")
                } else MockResponse().setBody("1")
            }
            val asiakas = DynastyClient(asetukset, jacksonObjectMapper(), kello)
            val pyynto = DynastyCaseRequest(DynastyCaseModel("schema", "Asia", DynastyAdditionalData()))
            asiakas.createCase("luokka", pyynto)
            whenever(kello.instant()).thenReturn(aika.aikaleima().plusSeconds(59))
            asiakas.createCase("luokka", pyynto)
            whenever(kello.instant()).thenReturn(aika.aikaleima().plusSeconds(60))
            asiakas.createCase("luokka", pyynto)

            val pyynnot = (0 until palvelin.requestCount).map { palvelin.takeRequest() }
            assertThat(pyynnot.count { it.path == "/token" }).isEqualTo(2)
            assertThat(pyynnot[1].getHeader("Authorization")).isEqualTo("Bearer ensimmainen")
            assertThat(pyynnot[2].getHeader("Authorization")).isEqualTo("Bearer ensimmainen")
            assertThat(pyynnot[4].getHeader("Authorization")).isEqualTo("Bearer toinen")
        }
    }
}
