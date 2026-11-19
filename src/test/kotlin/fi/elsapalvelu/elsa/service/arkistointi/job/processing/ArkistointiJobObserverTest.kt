package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.slf4j.LoggerFactory

class ArkistointiJobObserverTest {
    private val registry = SimpleMeterRegistry()
    private val alerts = mock<AlertPublisherService>()
    private val metrics = ArkistointiMetricsService(registry)
    private val observer = ArkistointiJobObserver(metrics, alerts, ApplicationProperties())

    @ParameterizedTest
    @CsvSource(
        "TURUN_YLIOPISTO, 1, Archive attempt failed",
        "ITA_SUOMEN_YLIOPISTO, 3, Archive delivery delayed",
        "TAMPEREEN_YLIOPISTO, 1, Archive attempt failed",
        "HELSINGIN_YLIOPISTO, 3, Archive delivery delayed"
    )
    fun `retry failures log at error level`(university: YliopistoEnum, attempt: Int, messagePrefix: String) {
        val logger = LoggerFactory.getLogger(ArkistointiJobObserver::class.java) as Logger
        val events = ListAppender<ILoggingEvent>()
        events.start()
        logger.addAppender(events)
        try {
            val failure = event(attempt).copy(university = university)
            observer.onCompleted(failure)

            val logged = events.list.single()
            assertThat(logged.level).isEqualTo(Level.ERROR)
            assertThat(logged.formattedMessage).startsWith(messagePrefix).contains(failure.jobId.toString(), "outcome=RETRYABLE_FAILURE")
            if (attempt == 3) verify(alerts).publishAlert(any(), any()) else verifyNoInteractions(alerts)
        } finally {
            logger.detachAppender(events)
            events.stop()
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 4, 9, 11, 19, 21, 24])
    fun `ordinary retry failures record metrics without alerts`(attempt: Int) {
        observer.onCompleted(event(attempt))
        verifyNoInteractions(alerts)
        assertThat(registry.find("arkistointi.errors.total").counter()).isNull()
        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "RETRYABLE_FAILURE").counter().count()).isEqualTo(1.0)
    }

    @ParameterizedTest
    @ValueSource(ints = [3, 10, 20])
    fun `threshold alert includes Finnish case details and transport diagnostics`(attempt: Int) {
        val event = event(attempt).copy(caseId = 17, opintooikeusId = 62, requestedByUserId = "test-vastuuhenkilo-tampere",
            errorDescription = "Louhi-siirto epaonnistui [stage=CONNECT, reason=AUTHENTICATION_FAILED, status=null]")
        observer.onCompleted(event)
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(org.mockito.kotlin.eq("Tampereen yliopisto: arkistointi viivästyy"), message.capture())
        assertThat(message.firstValue).contains(event.jobId.toString(), "Yritys: $attempt", "UNAVAILABLE",
            "Asia: Valmistumispyyntö", "Asian tunniste: 17", "Opinto-oikeuden tunniste: 62",
            "Arkistointipalvelu: Louhi", "CONNECT / AUTHENTICATION_FAILED", "test-vastuuhenkilo-tampere")
        assertThat(message.firstValue).doesNotContain("Ympäristö", "Toimintaohje", "Automaattiset")
        assertThat(registry.find("arkistointi.errors.total").counter()).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 10, 20, 25])
    fun `terminal outcomes always alert without a duplicate threshold warning`(attempt: Int) {
        observer.onCompleted(event(attempt).copy(result = ArkistointiAttemptCompleted.Result.TERMINAL_FAILURE))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(org.mockito.kotlin.eq("Tampereen yliopisto: arkistointi vaatii toimenpiteitä"), message.capture())
        assertThat(message.firstValue).contains("Yritys: $attempt")
        assertThat(registry.get("arkistointi.errors.total").counter().count()).isEqualTo(1.0)
        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "TERMINAL_FAILURE").counter().count()).isEqualTo(1.0)
    }

    @Test
    fun `success records completion without claiming recovery notification delivery`() {
        observer.onCompleted(event(4).copy(result = ArkistointiAttemptCompleted.Result.SUCCESS, errorCode = null))
        verifyNoInteractions(alerts)
        assertThat(registry.get("arkistointi.requests.total").counter().count()).isEqualTo(1.0)
        assertThat(registry.find("arkistointi.errors.total").counter()).isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `retries count as attempts while only the final failure counts as a failed operation`(succeeds: Boolean) {
        val first = event(1)
        observer.onCompleted(first)
        observer.onCompleted(first.copy(attempt = 2))
        observer.onCompleted(first.copy(attempt = 3))
        observer.onCompleted(first.copy(attempt = 4,
            result = if (succeeds) ArkistointiAttemptCompleted.Result.SUCCESS
                else ArkistointiAttemptCompleted.Result.TERMINAL_FAILURE,
            errorCode = if (succeeds) null else first.errorCode))

        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "RETRYABLE_FAILURE").counter().count()).isEqualTo(3.0)
        assertThat(registry.find("arkistointi.errors.total").counter()?.count() ?: 0.0).isEqualTo(if (succeeds) 0.0 else 1.0)
        assertThat(registry.find("arkistointi.requests.total").counter()?.count() ?: 0.0).isEqualTo(if (succeeds) 1.0 else 0.0)
        assertThat(registry.find("arkistointi.async.attempts.total").counters().sumOf { it.count() }).isEqualTo(4.0)
    }

    @Test
    fun `alert threshold is configurable`() {
        val properties = ApplicationProperties().apply { getArkistointi().retry.alertAttempts = listOf(2, 5) }
        val configured = ArkistointiJobObserver(metrics, alerts, properties)
        configured.onCompleted(event(1))
        verifyNoInteractions(alerts)
        configured.onCompleted(event(2))
        configured.onCompleted(event(3))
        configured.onCompleted(event(4))
        verify(alerts).publishAlert(any(), any())
        configured.onCompleted(event(5))
        verify(alerts, org.mockito.kotlin.times(2)).publishAlert(any(), any())
    }

    @Test
    fun `SNS or metrics failure never escapes the observer`() {
        doThrow(IllegalStateException("private SNS details")).`when`(alerts).publishAlert(any(), any())
        assertDoesNotThrow { observer.onCompleted(event(3)) }
        val brokenMetrics = mock<ArkistointiMetricsService>()
        doThrow(IllegalStateException("private metrics details")).`when`(brokenMetrics).recordAsyncAttempt(any(), any(), any())
        assertDoesNotThrow { ArkistointiJobObserver(brokenMetrics, alerts, ApplicationProperties()).onCompleted(event(3)) }
    }

    @Test
    fun `malformed error code cannot inject arbitrary text into alert`() {
        observer.onCompleted(event(3).copy(errorCode = "private/file.pdf\nsecret", errorDescription = "private/file.pdf\nsecret", requestedByUserId = "secret\ninjected"))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), message.capture())
        assertThat(message.firstValue).contains("ARCHIVE_ERROR_UNSPECIFIED").doesNotContain("private", "secret", "file.pdf")
    }

    @Test
    fun `Helsinki alert includes Siilo HTTP status and Finnish contract name`() {
        observer.onCompleted(event(3).copy(university = YliopistoEnum.HELSINGIN_YLIOPISTO, caseType = CaseType.SOPIMUS,
            errorCode = "SIILO_HTTP_REJECTED", errorDescription = "Siilo-toimitus epaonnistui [status=503, category=null]"))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(org.mockito.kotlin.eq("Helsingin yliopisto: arkistointi viivästyy"), message.capture())
        assertThat(message.firstValue).contains("Arkistointipalvelu: Siilo", "Asia: Koejakson koulutussopimus", "HTTP 503")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "LOUHI_TRANSFER_FAILED: Louhi-siirto epaonnistui [stage=CONNECT, reason=AUTHENTICATION_FAILED, status=null]",
        "SIILO_HTTP_REJECTED: Siilo-toimitus epaonnistui [status=503, category=null]"
    ])
    fun `exhausted retries retain the original transport diagnostics in the terminal alert`(failure: String) {
        observer.onCompleted(event(25).copy(result = ArkistointiAttemptCompleted.Result.TERMINAL_FAILURE,
            errorCode = "ARCHIVE_RETRIES_EXHAUSTED",
            errorDescription = "Arkistoinnin enimmainen yritysmaara saavutettiin. $failure"))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), message.capture())
        assertThat(message.firstValue).contains("ARCHIVE_RETRIES_EXHAUSTED",
            if (failure.startsWith("LOUHI")) "CONNECT / AUTHENTICATION_FAILED" else "HTTP 503")
    }

    private fun event(attempt: Int) = ArkistointiAttemptCompleted(
        UUID.randomUUID(), YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN,
        attempt, ArkistointiAttemptCompleted.Result.RETRYABLE_FAILURE, "UNAVAILABLE"
    )
}
