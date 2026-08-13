package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class SisuTreFetchingServiceLoggingTest {

    @Test
    fun `attainments HTTP error includes internal user ID but not HETU`() {
        val server = MockWebServer()
        server.start()
        val logger = LoggerFactory.getLogger(
            SisuTreOpintosuorituksetFetchingServiceImpl::class.java
        ) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody(
                        """{"statusCode":500,"message":"Internal server error","activityId":"activity-id"}"""
                    )
            )
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuTre().endpointUrl = server.url("/elsa").toString().removeSuffix("/")
            }
            val service = SisuTreOpintosuorituksetFetchingServiceImpl(
                object : OkHttpClientBuilder {
                    override fun okHttpClient() = OkHttpClient()
                },
                properties,
                jacksonObjectMapper(),
                Mockito.mock(YliopistoRepository::class.java)
            )
            MDC.put(MDC_USER_ID_KEY, "elsa-user-id")

            runBlocking { service.fetchOpintosuoritukset("010190-1234") }

            assertThat(appender.list.last().formattedMessage)
                .contains("/elsa/attainments userId=elsa-user-id")
                .doesNotContain("010190-1234")
        } finally {
            MDC.remove(MDC_USER_ID_KEY)
            logger.detachAppender(appender)
            appender.stop()
            server.shutdown()
        }
    }
}
