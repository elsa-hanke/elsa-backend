package fi.elsapalvelu.elsa.service.integration.peppi

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
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
    fun `successful empty result is logged as info for Turku and UEF without HETU`() {
        listOf(
            YliopistoEnum.TURUN_YLIOPISTO,
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO
        ).forEach { university ->
            val server = MockWebServer().apply {
                start()
                enqueue(
                    MockResponse().setBody(
                        """{"birthDate":"1990-01-01","entitlements":[""" +
                            """{"opiskeluoikeusNumero":"study-right","koulutusKoodi":"unsupported","erikoisalat":[]}]}"""
                    )
                )
            }
            try {
                val event = captureLog(PeppiCommonOpintotietodataFetchingServiceImpl::class.java) {
                    runBlocking {
                        PeppiCommonOpintotietodataFetchingServiceImpl(jacksonObjectMapper())
                            .fetchOpintotietodata(
                                server.url("/student").toString(),
                                OkHttpClient(),
                                TEST_HETU,
                                university
                            )
                    }
                }

                assertThat(event.level).isEqualTo(Level.INFO)
                assertThat(event.formattedMessage)
                    .contains("$university: opintotietokysely onnistui")
                    .contains("Lähdejärjestelmän opinto-oikeuksia: 1")
                    .contains("userId=$TEST_USER_ID")
                    .doesNotContain(TEST_HETU)
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun `study-right HTTP error includes internal user ID but not HETU`() {
        val server = failingServer()
        try {
            val event = captureLog(PeppiCommonOpintotietodataFetchingServiceImpl::class.java) {
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

            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments HTTP error includes internal user ID but not HETU`() {
        val server = failingServer()
        try {
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
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

            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments empty JSON object logs warning with keys and returns null`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        }
        try {
            var result: OpintosuorituksetPersistenceDTO? = null
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                runBlocking {
                    result = PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                        .fetchOpintosuoritukset(
                            server.url("/attainments").toString(),
                            OkHttpClient(),
                            TEST_HETU,
                            YliopistoEnum.TURUN_YLIOPISTO
                        )
                }
            }

            assertThat(result).isNull()
            assertThat(event.level).isEqualTo(Level.WARN)
            assertThat(event.formattedMessage)
                .contains("Vastaanotettiin OBJECT JSON-taulukon sijaan")
                .contains("avaimet=[]")
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments unexpected object with HTTP 200 logs error with summary and returns null`() {
        val errorBody = """{"fault":{"code":900901,"message":"Service Unavailable"}}"""
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setResponseCode(200).setBody(errorBody))
        }
        try {
            var result: OpintosuorituksetPersistenceDTO? = null
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                runBlocking {
                    result = PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                        .fetchOpintosuoritukset(
                            server.url("/attainments").toString(),
                            OkHttpClient(),
                            TEST_HETU,
                            YliopistoEnum.TURUN_YLIOPISTO
                        )
                }
            }

            assertThat(result).isNull()
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage)
                .contains("Vastaanotettiin OBJECT JSON-taulukon sijaan")
                .contains("avaimet=[fault]")
                .contains("Service Unavailable")
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments invalid item field inside array throws JsonProcessingException without leaking payload`() {
        val secretCourseTitle = "ErittäinSalainenErikoiskurssi"
        val server = MockWebServer().apply {
            start()
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """[{"opintopisteet":"not-a-number","kurssiKoodi":"ELOP0001","nimi":{"fi":"$secretCourseTitle"}}]"""
                )
            )
        }
        try {
            var thrown: Exception? = null
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                try {
                    runBlocking {
                        PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                            .fetchOpintosuoritukset(
                                server.url("/attainments").toString(),
                                OkHttpClient(),
                                TEST_HETU,
                                YliopistoEnum.TURUN_YLIOPISTO
                            )
                    }
                } catch (e: Exception) {
                    thrown = e
                }
            }

            assertThat(thrown).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage)
                .contains("opintopisteet")
                .doesNotContain(secretCourseTitle)
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments malformed JSON logs error with body and throws JsonProcessingException`() {
        val malformedBody = "<html><body>502 Bad Gateway</body></html>"
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setResponseCode(200).setBody(malformedBody))
        }
        try {
            var thrown: Exception? = null
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                try {
                    runBlocking {
                        PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                            .fetchOpintosuoritukset(
                                server.url("/attainments").toString(),
                                OkHttpClient(),
                                TEST_HETU,
                                YliopistoEnum.TURUN_YLIOPISTO
                            )
                    }
                } catch (e: Exception) {
                    thrown = e
                }
            }

            assertThat(thrown).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).contains("502 Bad Gateway")
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments response body with HETU is redacted in log message`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setResponseCode(500).setBody("Failed for user $TEST_HETU"))
        }
        try {
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
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

            assertThat(event.formattedMessage)
                .contains("[REDACTED_HETU]")
                .doesNotContain(TEST_HETU)
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `attainments primitive string JSON with HTTP 200 logs error and returns null`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setResponseCode(200).setBody("\"Service unavailable\""))
        }
        try {
            var result: OpintosuorituksetPersistenceDTO? = null
            val event = captureLog(PeppiCommonOpintosuorituksetFetchingServiceImpl::class.java) {
                runBlocking {
                    result = PeppiCommonOpintosuorituksetFetchingServiceImpl(jacksonObjectMapper())
                        .fetchOpintosuoritukset(
                            server.url("/attainments").toString(),
                            OkHttpClient(),
                            TEST_HETU,
                            YliopistoEnum.TURUN_YLIOPISTO
                        )
                }
            }

            assertThat(result).isNull()
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).contains("Vastaanotettiin STRING JSON-taulukon sijaan")
            assertUserCorrelation(event.formattedMessage)
        } finally {
            server.shutdown()
        }
    }

    private fun failingServer() = MockWebServer().apply {
        start()
        enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))
    }

    private fun captureLog(loggerClass: Class<*>, block: () -> Unit): ILoggingEvent {
        val logger = LoggerFactory.getLogger(loggerClass) as Logger
        val appender = ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
        return try {
            MDC.putCloseable(MDC_USER_ID_KEY, TEST_USER_ID).use { block() }
            appender.list.last()
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
