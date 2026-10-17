package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class IntegrationAlertServiceTest {

    private val alertPublisherService = Mockito.mock(AlertPublisherService::class.java)
    private val alertService = IntegrationAlertService(alertPublisherService)

    @Test
    fun `connectivity alert is published after five consecutive failures`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }

        verify(alertPublisherService, never()).publishAlert(any(), any())

        recordFailure(STUDY_RIGHTS_ENDPOINT)
        recordFailure(STUDY_RIGHTS_ENDPOINT)

        verify(alertPublisherService, times(1)).publishAlert(any(), any())
    }

    @Test
    fun `successful connection resets failures before alert threshold`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }
        alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `connectivity failures are counted separately for each endpoint`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
            recordFailure(ATTAINMENTS_ENDPOINT)
        }

        verify(alertPublisherService, never()).publishAlert(any(), any())

        recordFailure(STUDY_RIGHTS_ENDPOINT)

        verify(alertPublisherService, times(1)).publishAlert(any(), any())
    }

    @Test
    fun `active connectivity alert is rearmed after three consecutive successes`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }
        repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD - 1) {
            alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)
        }
        recordFailure(STUDY_RIGHTS_ENDPOINT)
        repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
            alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)
        }
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }

        // 1 failure alert + 1 recovery alert (after 3 consecutive successes) + 1 failure alert again.
        verify(alertPublisherService, times(3)).publishAlert(any(), any())
    }

    @Test
    fun `recovery alert is published once connectivity alert threshold is reached`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }
        verify(alertPublisherService, times(1)).publishAlert(any(), any())

        repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD - 1) {
            alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)
        }
        // Not yet enough consecutive successes to consider it recovered.
        verify(alertPublisherService, times(1)).publishAlert(any(), any())

        alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)

        verify(alertPublisherService, times(2)).publishAlert(any(), any())
        verify(alertPublisherService).publishAlert(
            check { assertThat(it).contains("korjaantunut") },
            check { assertThat(it).contains(STUDY_RIGHTS_ENDPOINT) }
        )
    }

    @Test
    fun `recovery alert is not published when alert never became active`() {
        repeat(IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD - 1) {
            recordFailure(STUDY_RIGHTS_ENDPOINT)
        }
        repeat(IntegrationAlertService.CONNECTIVITY_RECOVERY_SUCCESS_THRESHOLD) {
            alertService.recordConnectivitySuccess(ALERT_KEY, STUDY_RIGHTS_ENDPOINT)
        }

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `authentication alert recovery is published once when marked successful`() {
        alertService.publishOnceUntilSuccess(ALERT_KEY, "Auth failed", "Auth failed message")
        verify(alertPublisherService, times(1)).publishAlert(any(), any())

        alertService.markSuccessful(ALERT_KEY)
        verify(alertPublisherService, times(2)).publishAlert(any(), any())
        verify(alertPublisherService).publishAlert(
            check { assertThat(it).contains("korjaantunut") },
            any()
        )

        // Marking successful again with no active alert must not publish another recovery alert.
        alertService.markSuccessful(ALERT_KEY)
        verify(alertPublisherService, times(2)).publishAlert(any(), any())
    }

    private fun recordFailure(endpoint: String) {
        alertService.recordConnectivityFailure(
            ALERT_KEY,
            endpoint,
            "Connection failed",
            "Connection failed"
        )
    }

    private companion object {
        val ALERT_KEY = IntegrationAlertKey.SISU_TRE_API_AUTHENTICATION
        const val STUDY_RIGHTS_ENDPOINT = "https://sisu.example.test/study-rights"
        const val ATTAINMENTS_ENDPOINT = "https://sisu.example.test/attainments"
    }
}

