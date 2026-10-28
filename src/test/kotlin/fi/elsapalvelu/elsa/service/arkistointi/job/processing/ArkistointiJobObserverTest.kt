package fi.elsapalvelu.elsa.service.arkistointi.job.processing

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
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class ArkistointiJobObserverTest {
    private val registry = SimpleMeterRegistry()
    private val alerts = mock<AlertPublisherService>()
    private val metrics = ArkistointiMetricsService(registry)
    private val observer = ArkistointiJobObserver(metrics, alerts, ApplicationProperties())

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 4, 24])
    fun `ordinary retry failures record metrics without alerts`(attempt: Int) {
        observer.onCompleted(event(attempt))
        verifyNoInteractions(alerts)
        assertThat(registry.find("arkistointi.errors.total").counter()).isNull()
        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "RETRYABLE_FAILURE").counter().count()).isEqualTo(1.0)
    }

    @Test
    fun `threshold warning says automatic retries continue and includes safe identity only`() {
        val event = event(3)
        observer.onCompleted(event)
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), message.capture())
        assertThat(message.firstValue).contains("Automaattiset yritykset jatkuvat", event.jobId.toString(), "attempt=3", "UNAVAILABLE")
        assertThat(registry.find("arkistointi.errors.total").counter()).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 25])
    fun `terminal outcomes always alert without a duplicate threshold warning`(attempt: Int) {
        observer.onCompleted(event(attempt).copy(outcome = ArkistointiAttemptCompleted.Outcome.TERMINAL_FAILURE))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), message.capture())
        assertThat(message.firstValue).contains("Automaattiset yritykset on lopetettu")
        assertThat(registry.get("arkistointi.errors.total").counter().count()).isEqualTo(1.0)
        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "TERMINAL_FAILURE").counter().count()).isEqualTo(1.0)
    }

    @Test
    fun `success records completion without claiming recovery notification delivery`() {
        observer.onCompleted(event(4).copy(outcome = ArkistointiAttemptCompleted.Outcome.SUCCESS, errorCode = null))
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
            outcome = if (succeeds) ArkistointiAttemptCompleted.Outcome.SUCCESS
                else ArkistointiAttemptCompleted.Outcome.TERMINAL_FAILURE,
            errorCode = if (succeeds) null else first.errorCode))

        assertThat(registry.get("arkistointi.async.attempts.total").tag("outcome", "RETRYABLE_FAILURE").counter().count()).isEqualTo(3.0)
        assertThat(registry.find("arkistointi.errors.total").counter()?.count() ?: 0.0).isEqualTo(if (succeeds) 0.0 else 1.0)
        assertThat(registry.find("arkistointi.requests.total").counter()?.count() ?: 0.0).isEqualTo(if (succeeds) 1.0 else 0.0)
        assertThat(registry.find("arkistointi.async.attempts.total").counters().sumOf { it.count() }).isEqualTo(4.0)
    }

    @Test
    fun `alert threshold is configurable`() {
        val properties = ApplicationProperties().apply { getArkistointi().retry.alertAfterAttempts = 2 }
        val configured = ArkistointiJobObserver(metrics, alerts, properties)
        configured.onCompleted(event(1))
        verifyNoInteractions(alerts)
        configured.onCompleted(event(2))
        verify(alerts).publishAlert(any(), any())
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
        observer.onCompleted(event(3).copy(errorCode = "private/file.pdf\nsecret"))
        val message = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), message.capture())
        assertThat(message.firstValue).contains("ARCHIVE_ERROR_UNSPECIFIED").doesNotContain("private", "secret", "file.pdf")
    }

    private fun event(attempt: Int) = ArkistointiAttemptCompleted(
        UUID.randomUUID(), YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN,
        attempt, ArkistointiAttemptCompleted.Outcome.RETRYABLE_FAILURE, "UNAVAILABLE"
    )
}
