package fi.elsapalvelu.elsa.service.integration.peppi.oulu

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.okHttpClient
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.IntegrationAuthenticationAlertInterceptor
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.core.io.DefaultResourceLoader

class PeppiOuluClientAndFailureTest {

    @Test
    fun `apollo client is constructed without runtime linkage errors`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getPeppiOulu().apply {
                token = "test-token"
                graphqlEndpointUrl = "https://example.invalid/graphql"
            }
        }
        val builder = PeppiOuluClientBuilderImpl(
            properties,
            DefaultResourceLoader(),
            IntegrationAlertService(Mockito.mock(AlertPublisherService::class.java))
        )

        assertThatCode { builder.apolloClient() }
            .doesNotThrowAnyException()
        assertThat(
            builder.okHttpClient().interceptors.any {
                it is IntegrationAuthenticationAlertInterceptor
            }
        ).isTrue
    }

    @Test
    fun `network failure is propagated as Apollo exception`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val clientBuilder = object : GraphQLClientBuilder {
                override fun okHttpClient() = OkHttpClient()
                override fun apolloClient(): ApolloClient = ApolloClient.Builder()
                    .serverUrl(server.url("/graphql").toString())
                    .build()
            }
            val service = PeppiOuluOpintotietodataFetchingServiceImpl(
                clientBuilder,
                IntegrationAlertService(Mockito.mock(AlertPublisherService::class.java)),
                Mockito.mock(YliopistoRepository::class.java)
            )

            assertThatThrownBy {
                runBlocking { service.fetchOpintotietodata("test-identity") }
            }.isInstanceOf(ApolloException::class.java)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `GraphQL authentication alert is suppressed until an authenticated response`() {
        val server = MockWebServer()
        server.start()
        try {
            val response = MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "errors": [{
                        "message": "Authentication failed",
                        "extensions": {"code": "UNAUTHENTICATED"}
                      }]
                    }
                    """.trimIndent()
            )
            server.enqueue(response)
            server.enqueue(response)
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"data":{"private_person_by_personal_identity_code":null}}""")
            )
            server.enqueue(response)
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val service = opintotietodataService(server, alertPublisherService)

            repeat(2) {
                assertThatThrownBy {
                    runBlocking { service.fetchOpintotietodata("test-identity") }
                }.isInstanceOf(RuntimeException::class.java)
            }
            assertThat(runBlocking { service.fetchOpintotietodata("test-identity") }).isNull()
            assertThatThrownBy {
                runBlocking { service.fetchOpintotietodata("test-identity") }
            }.isInstanceOf(RuntimeException::class.java)

            // Initial failure, recovery after the authenticated response, and a new failure.
            verify(alertPublisherService, times(3)).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `other GraphQL error does not publish an authentication alert`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "errors": [{
                            "message": "Person was not found",
                            "extensions": {"code": "BAD_USER_INPUT"}
                          }]
                        }
                        """.trimIndent()
                    )
            )
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val service = opintotietodataService(server, alertPublisherService)

            assertThatThrownBy {
                runBlocking { service.fetchOpintotietodata("test-identity") }
            }.isInstanceOf(RuntimeException::class.java)

            verify(alertPublisherService, never()).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `No value present GraphQL error never publishes an alert regardless of count`() {
        val server = MockWebServer()
        server.start()
        try {
            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD + 5) {
                server.enqueue(noValuePresentErrorResponse())
            }
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val service = opintotietodataService(server, alertPublisherService)

            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD + 5) {
                assertThat(runBlocking { service.fetchOpintotietodata("test-identity") }).isNull()
            }

            verify(alertPublisherService, never()).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `InvocationTargetException GraphQL error does not throw and is suppressed below the threshold`() {
        val server = MockWebServer()
        server.start()
        try {
            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
                server.enqueue(invocationTargetExceptionErrorResponse())
            }
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val service = opintotietodataService(server, alertPublisherService)

            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
                assertThat(runBlocking { service.fetchOpintotietodata("test-identity") }).isNull()
            }

            verify(alertPublisherService, never()).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `InvocationTargetException GraphQL error publishes an alert after the failure threshold and recovers`() {
        val server = MockWebServer()
        server.start()
        try {
            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
                server.enqueue(invocationTargetExceptionErrorResponse())
            }
            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                server.enqueue(successResponse())
            }
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val service = opintotietodataService(server, alertPublisherService)

            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
                assertThat(runBlocking { service.fetchOpintotietodata("test-identity") }).isNull()
            }
            // Failure alert published exactly once after reaching the threshold.
            verify(alertPublisherService, times(1)).publishAlert(any(), any())

            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                assertThat(runBlocking { service.fetchOpintotietodata("test-identity") }).isNull()
            }
            // Recovery alert published once consecutive successes reach the recovery threshold.
            verify(alertPublisherService, times(2)).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    private fun noValuePresentErrorResponse(): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "errors": [{
                    "message": "Unexpected Internal Error: No value present",
                    "path": ["private_person_by_personal_identity_code", "dateOfBirth"]
                  }]
                }
                """.trimIndent()
            )

    private fun invocationTargetExceptionErrorResponse(): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "errors": [{
                    "message": "Unexpected Internal Error: java.lang.reflect.InvocationTargetException",
                    "path": ["private_person_by_personal_identity_code", "dateOfBirth"],
                    "extensions": {"errorType": "GraphQLException", "errorCode": "Unexpected Internal Error"}
                  }]
                }
                """.trimIndent()
            )

    private fun successResponse(): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""{"data":{"private_person_by_personal_identity_code":null}}""")

    private fun opintotietodataService(
        server: MockWebServer,
        alertPublisherService: AlertPublisherService
    ): PeppiOuluOpintotietodataFetchingServiceImpl {
        val integrationAlertService = IntegrationAlertService(alertPublisherService)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                IntegrationAuthenticationAlertInterceptor(
                    integrationAlertService,
                    IntegrationAlertKey.PEPPI_OULU_AUTHENTICATION,
                    "Oulun Peppi",
                    resetOnSuccessfulResponse = false
                )
            )
            .build()
        val clientBuilder = object : GraphQLClientBuilder {
            override fun okHttpClient() = okHttpClient
            override fun apolloClient(): ApolloClient = ApolloClient.Builder()
                .serverUrl(server.url("/graphql").toString())
                .okHttpClient(okHttpClient)
                .build()
        }
        return PeppiOuluOpintotietodataFetchingServiceImpl(
            clientBuilder,
            integrationAlertService,
            Mockito.mock(YliopistoRepository::class.java)
        )
    }
}
