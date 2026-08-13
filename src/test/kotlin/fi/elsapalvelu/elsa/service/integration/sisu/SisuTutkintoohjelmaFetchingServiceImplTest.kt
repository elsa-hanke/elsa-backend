package fi.elsapalvelu.elsa.service.integration.sisu

import com.apollographql.apollo.ApolloClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SisuTutkintoohjelmaFetchingServiceImplTest {

    @Test
    fun `HETU-independent export failure alerts once until a successful export`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""{"entities":[]}"""))
            server.enqueue(MockResponse().setResponseCode(404))
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val client = OkHttpClient()
            val clientBuilder = object : GraphQLClientBuilder {
                override fun okHttpClient() = client
                override fun apolloClient(): ApolloClient = error("Not used by the export")
            }
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuHy().tutkintoohjelmaExportUrl = server.url("/qualifications").toString()
            }
            val service = SisuTutkintoohjelmaFetchingServiceImpl(
                clientBuilder,
                properties,
                jacksonObjectMapper(),
                IntegrationAlertService(alertPublisherService)
            )

            assertThat(runBlocking { service.fetch() }).isNull()
            assertThat(runBlocking { service.fetch() }).isNull()
            assertThat(runBlocking { service.fetch() }).isNotNull
            assertThat(runBlocking { service.fetch() }).isNull()

            verify(alertPublisherService, times(2)).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `internal server error does not publish export alert`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(500))
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val clientBuilder = object : GraphQLClientBuilder {
                override fun okHttpClient() = OkHttpClient()
                override fun apolloClient(): ApolloClient = error("Not used by the export")
            }
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuHy().tutkintoohjelmaExportUrl =
                    server.url("/qualifications").toString()
            }
            val service = SisuTutkintoohjelmaFetchingServiceImpl(
                clientBuilder,
                properties,
                jacksonObjectMapper(),
                IntegrationAlertService(alertPublisherService)
            )

            assertThat(runBlocking { service.fetch() }).isNull()

            verify(alertPublisherService, never()).publishAlert(any(), any())
        } finally {
            server.shutdown()
        }
    }
}
