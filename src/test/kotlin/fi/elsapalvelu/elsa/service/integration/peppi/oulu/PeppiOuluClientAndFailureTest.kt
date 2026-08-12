package fi.elsapalvelu.elsa.service.integration.peppi.oulu

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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
        val builder = PeppiOuluClientBuilderImpl(properties, DefaultResourceLoader())

        assertThatCode { builder.apolloClient() }
            .doesNotThrowAnyException()
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
                Mockito.mock(YliopistoRepository::class.java)
            )

            assertThatThrownBy {
                runBlocking { service.fetchOpintotietodata("test-identity") }
            }.isInstanceOf(ApolloException::class.java)
        } finally {
            server.shutdown()
        }
    }
}
