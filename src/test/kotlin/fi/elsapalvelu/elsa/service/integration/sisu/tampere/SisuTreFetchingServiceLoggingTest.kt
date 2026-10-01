package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class SisuTreFetchingServiceLoggingTest {

    @Test
    fun `unrecognized study rights are logged as warning without HETU`() {
        val server = MockWebServer().apply {
            start()
            enqueue(
                MockResponse().setBody(
                    """{"studentNumber":"student","dateOfBirth":"1959-11-08","studyrights":[""" +
                        """{"id":"study-right","state":"ACTIVE","phase1EducationClassificationUrn":null,"specialisation":null}]}"""
                )
            )
        }
        val logger = LoggerFactory.getLogger(SisuTreOpintotietodataFetchingServiceImpl::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        try {
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuTre().endpointUrl = server.url("/elsa").toString().removeSuffix("/")
            }
            val service = SisuTreOpintotietodataFetchingServiceImpl(
                object : OkHttpClientBuilder {
                    override fun okHttpClient() = OkHttpClient()
                },
                properties,
                jacksonObjectMapper(),
                IntegrationAlertService(Mockito.mock(AlertPublisherService::class.java)),
                Mockito.mock(YliopistoRepository::class.java)
            )
            MDC.put(MDC_USER_ID_KEY, "elsa-user-id")

            val result = runBlocking { service.fetchOpintotietodata(TEST_HETU) }

            assertThat(result?.opintooikeudet).isEmpty()
            assertThat(appender.list.last().level).isEqualTo(Level.WARN)
            assertThat(appender.list.last().formattedMessage)
                .contains("Tampereen Sisusta saatiin 1 opinto-oikeutta")
                .contains("userId=elsa-user-id")
                .doesNotContain(TEST_HETU)
        } finally {
            MDC.remove(MDC_USER_ID_KEY)
            logger.detachAppender(appender)
            appender.stop()
            server.shutdown()
        }
    }

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
                IntegrationAlertService(Mockito.mock(AlertPublisherService::class.java)),
                Mockito.mock(YliopistoRepository::class.java)
            )
            MDC.put(MDC_USER_ID_KEY, "elsa-user-id")

            runBlocking { service.fetchOpintosuoritukset(TEST_HETU) }

            assertThat(appender.list.last().formattedMessage)
                .contains("/elsa/attainments userId=elsa-user-id")
                .doesNotContain(TEST_HETU)
        } finally {
            MDC.remove(MDC_USER_ID_KEY)
            logger.detachAppender(appender)
            appender.stop()
            server.shutdown()
        }
    }

    @Test
    fun `attainments internal server error alerts after ten failures and recovers`() {
        val server = MockWebServer()
        server.start()
        try {
            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                        .setBody(
                            """{"statusCode":500,"message":"Internal server error","activityId":"activity-id"}"""
                        )
                )
            }
            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                server.enqueue(MockResponse().setBody("""{"attainments":[]}"""))
            }
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuTre().endpointUrl = server.url("/elsa").toString().removeSuffix("/")
            }
            val service = SisuTreOpintosuorituksetFetchingServiceImpl(
                object : OkHttpClientBuilder {
                    override fun okHttpClient() = OkHttpClient()
                },
                properties,
                jacksonObjectMapper(),
                IntegrationAlertService(alertPublisherService),
                Mockito.mock(YliopistoRepository::class.java)
            )

            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
                assertThat(runBlocking { service.fetchOpintosuoritukset(TEST_HETU) }).isNull()
            }
            verify(alertPublisherService, never()).publishAlert(any(), any())

            assertThat(runBlocking { service.fetchOpintosuoritukset(TEST_HETU) }).isNull()
            verify(alertPublisherService, times(1)).publishAlert(
                check { assertThat(it).contains("attainments") },
                check { assertThat(it).contains("HTTP status: 500") }
            )

            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                assertThat(runBlocking { service.fetchOpintosuoritukset(TEST_HETU) }).isNotNull
            }
            verify(alertPublisherService, times(2)).publishAlert(any(), any())
            verify(alertPublisherService).publishAlert(
                check { assertThat(it).contains("korjaantunut") },
                any()
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `study-rights internal server error alerts after ten failures and recovers`() {
        val server = MockWebServer()
        server.start()
        try {
            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                        .setBody(
                            """{"statusCode":500,"message":"Internal server error","activityId":"activity-id"}"""
                        )
                )
            }
            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                server.enqueue(
                    MockResponse().setBody(
                        """{"studentNumber":"student","dateOfBirth":"1959-11-08","studyrights":[]}"""
                    )
                )
            }
            val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
            val properties = ApplicationProperties().apply {
                getSecurity().getSisuTre().endpointUrl = server.url("/elsa").toString().removeSuffix("/")
            }
            val service = SisuTreOpintotietodataFetchingServiceImpl(
                object : OkHttpClientBuilder {
                    override fun okHttpClient() = OkHttpClient()
                },
                properties,
                jacksonObjectMapper(),
                IntegrationAlertService(alertPublisherService),
                Mockito.mock(YliopistoRepository::class.java)
            )

            repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
                assertThat(runBlocking { service.fetchOpintotietodata(TEST_HETU) }).isNull()
            }
            verify(alertPublisherService, never()).publishAlert(any(), any())

            assertThat(runBlocking { service.fetchOpintotietodata(TEST_HETU) }).isNull()
            verify(alertPublisherService, times(1)).publishAlert(
                check { assertThat(it).contains("study-rights") },
                check { assertThat(it).contains("HTTP status: 500") }
            )

            repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
                assertThat(runBlocking { service.fetchOpintotietodata(TEST_HETU) }).isNotNull
            }
            verify(alertPublisherService, times(2)).publishAlert(any(), any())
            verify(alertPublisherService).publishAlert(
                check { assertThat(it).contains("korjaantunut") },
                any()
            )
        } finally {
            server.shutdown()
        }
    }

    private companion object {
        const val TEST_HETU = "010190-1234"
    }
}
