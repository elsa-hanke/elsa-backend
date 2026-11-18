package fi.elsapalvelu.elsa.service.integration.sisu.helsinki

import com.apollographql.apollo.ApolloClient
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class SisuHyAuthenticationAlertTest {

    private val server = MockWebServer()
    private val publisher = mock<AlertPublisherService>()
    private lateinit var apolloClient: ApolloClient
    private lateinit var studyRightsService: SisuHyOpintotietodataFetchingServiceImpl
    private lateinit var attainmentsService: SisuHyOpintosuorituksetFetchingServiceImpl

    @BeforeEach
    fun setUp() {
        server.start()
        apolloClient = ApolloClient.Builder()
            .serverUrl(server.url("/graphql").toString())
            .build()
        val clientBuilder = object : GraphQLClientBuilder {
            override fun okHttpClient() = OkHttpClient()
            override fun apolloClient() = apolloClient
        }
        val alerts = IntegrationAlertService(publisher)
        val repository = mock<YliopistoRepository>()
        studyRightsService = SisuHyOpintotietodataFetchingServiceImpl(clientBuilder, alerts, repository)
        attainmentsService = SisuHyOpintosuorituksetFetchingServiceImpl(clientBuilder, alerts, repository)
    }

    @AfterEach
    fun tearDown() {
        apolloClient.close()
        server.shutdown()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `successful response does not publish authentication alert`(fetchAttainments: Boolean) {
        enqueueSuccessfulResponse()

        fetch(fetchAttainments)

        verifyNoInteractions(publisher)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `authentication alert is suppressed until recovery and then rearmed`(fetchAttainments: Boolean) {
        repeat(2) {
            enqueueUnauthenticatedResponse()
            assertThatThrownBy { fetch(fetchAttainments) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("UNAUTHENTICATED")
        }
        verify(publisher, times(1)).publishAlert(eq(ALERT_SUBJECT), any())

        enqueueSuccessfulResponse()
        fetch(fetchAttainments)
        verify(publisher).publishAlert(eq("$ALERT_SUBJECT - tilanne korjaantunut"), any())

        enqueueUnauthenticatedResponse()
        assertThatThrownBy { fetch(fetchAttainments) }
            .isInstanceOf(RuntimeException::class.java)
        verify(publisher, times(2)).publishAlert(eq(ALERT_SUBJECT), any())
        verify(publisher, times(3)).publishAlert(any(), any())
    }

    private fun fetch(fetchAttainments: Boolean) {
        runBlocking {
            if (fetchAttainments) {
                attainmentsService.fetchOpintosuoritukset("test-identity")
            } else {
                studyRightsService.fetchOpintotietodata("test-identity")
            }
        }
    }

    private fun enqueueSuccessfulResponse() {
        enqueueResponse("""{"data":{"private_person_by_personal_identity_code":null}}""")
    }

    private fun enqueueUnauthenticatedResponse() {
        enqueueResponse(
            """{"errors":[{"message":"Authentication failed","extensions":{"code":"UNAUTHENTICATED"}}]}"""
        )
    }

    private fun enqueueResponse(body: String) {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
    }

    private companion object {
        const val ALERT_SUBJECT = "Helsingin Sisu opintotietointegraation autentikointi epäonnistui"
    }
}
