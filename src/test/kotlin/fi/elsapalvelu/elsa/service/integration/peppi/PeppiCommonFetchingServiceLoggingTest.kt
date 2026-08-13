package fi.elsapalvelu.elsa.service.integration.peppi

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class PeppiCommonFetchingServiceLoggingTest {

    @Test
    fun `study-right HTTP error includes internal user ID but not HETU`() {
        val server = failingServer()
        try {
            val message = captureLog(PeppiCommonOpintotietodataFetchingServiceImpl::class.java) {
                runBlocking {
                    PeppiCommonOpintotietodataFetchingServiceImpl(jacksonObjectMapper())
                        .fetchOpintotietodata(
                            server.url("/student").toString(),
                            OkHttpClient(),
                            TEST_HETU,
                            YliopistoEnum.TURUN_YLIOPISTO
                        )
                }
            }

            assertUserCorrelation(message)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments HTTP error includes internal user ID but not HETU`() {
        val server = failingServer()
        try {
            val message = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                runBlocking {
                    PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                        .fetchOpintosuoritukset(
                            server.url("/attainments").toString(),
                            OkHttpClient(),
                            TEST_HETU,
                            YliopistoEnum.TURUN_YLIOPISTO
                        )
                }
            }

            assertUserCorrelation(message)
        } finally {
            server.shutdown()
        }
    }

    private fun failingServer() = MockWebServer().apply {
        start()
        enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))
    }

    private fun captureLog(loggerClass: Class<*>, block: () -> Unit): String {
        val logger = LoggerFactory.getLogger(loggerClass) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        return try {
            MDC.putCloseable(MDC_USER_ID_KEY, TEST_USER_ID).use { block() }
            appender.list.last().formattedMessage
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun assertUserCorrelation(message: String) {
        assertThat(message)
            .contains("userId=$TEST_USER_ID")
            .doesNotContain(TEST_HETU)
    }

    private companion object {
        const val TEST_USER_ID = "elsa-user-id"
        const val TEST_HETU = "010190-1234"
    }
}
