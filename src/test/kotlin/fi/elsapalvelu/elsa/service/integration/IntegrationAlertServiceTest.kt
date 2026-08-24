package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
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
