package fi.elsapalvelu.elsa.service.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.AlertPublisherService
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.arkistointi.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.arkistointi.TampereLouhiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Unit tests verifying that [ArkistointiServiceImpl.laheta] publishes an alert via
 * [AlertPublisherService] whenever Helsinki (HY Siilo) or Tampere (Louhi SFTP) archiving fails,
 * and that no alert is published on success.
 */
@ExtendWith(MockitoExtension::class)
class ArkistointiAlertTest {

    @Mock
    private lateinit var alertPublisherService: AlertPublisherService

    @Mock
    private lateinit var tampereLouhiService: TampereLouhiService

    @Mock
    private lateinit var helsinkiSiiloService: HelsinkiSiiloService

    private lateinit var arkistointiService: ArkistointiServiceImpl

    @BeforeEach
    fun setUp() {
        val applicationProperties = ApplicationProperties()
        applicationProperties.getArkistointi().getTre().host = "localhost"
        applicationProperties.getArkistointi().getTre().port = "22"
        applicationProperties.getArkistointi().getTre().user = "test-user"

        arkistointiService = ArkistointiServiceImpl(
            applicationProperties = applicationProperties,
            tampereLouhiService = tampereLouhiService,
            helsinkiSiiloService = helsinkiSiiloService,
            alertPublisherService = alertPublisherService,
            arkistointiMetrics = ArkistointiMetricsService(SimpleMeterRegistry())
        )
    }

    // -------------------------------------------------------------------------
    // Helsinki (HY Siilo)
    // -------------------------------------------------------------------------

    @Test
    fun `alert is published when Helsinki archiving fails`() {
        whenever(helsinkiSiiloService.laheta(any(), any()))
            .thenThrow(RuntimeException("HY Siilo connection refused"))

        assertThrows(RuntimeException::class.java) {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                filePath = "/tmp/test.zip",
                caseType = CaseType.VALMISTUMINEN,
                yek = false
            )
        }

        val subjectCaptor = argumentCaptor<String>()
        val messageCaptor = argumentCaptor<String>()
        verify(alertPublisherService).publishAlert(subjectCaptor.capture(), messageCaptor.capture())

        assertThat(subjectCaptor.firstValue).contains("Helsinki")
        assertThat(messageCaptor.firstValue).contains("HY Siilo connection refused")
    }

    @Test
    fun `alert is published when Helsinki archiving fails with non-200 response`() {
        whenever(helsinkiSiiloService.laheta(any(), any()))
            .thenThrow(RuntimeException("HY arkistointi epäonnistui: HTTP 503 Service Unavailable, URL: http://esb-api.it.helsinki.fi/unisign/elsa/archive/abc123. Palvelimen vastaus: (tyhjä)"))

        assertThrows(RuntimeException::class.java) {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                filePath = "/tmp/hy.zip",
                caseType = CaseType.KOEJAKSO,
                yek = false
            )
        }

        val subjectCaptor = argumentCaptor<String>()
        val messageCaptor = argumentCaptor<String>()
        verify(alertPublisherService).publishAlert(subjectCaptor.capture(), messageCaptor.capture())

        assertThat(subjectCaptor.firstValue).contains("Helsinki")
        assertThat(messageCaptor.firstValue).contains("HTTP 503")
        assertThat(messageCaptor.firstValue).contains("URL: http://esb-api.it.helsinki.fi")
    }

    @Test
    fun `no alert is published when Helsinki archiving succeeds`() {
        // laheta is a void method – default mock behaviour is a no-op, which is success

        arkistointiService.laheta(
            yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
            filePath = "/tmp/ok.zip",
            caseType = CaseType.VALMISTUMINEN,
            yek = false
        )

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    // -------------------------------------------------------------------------
    // Tampere (Louhi SFTP)
    // -------------------------------------------------------------------------

    @Test
    fun `alert is published when Tampere archiving fails`() {
        whenever(tampereLouhiService.laheta(any(), any()))
            .thenThrow(RuntimeException("SFTP connection timed out"))

        assertThrows(RuntimeException::class.java) {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                filePath = "/tmp/tre.zip",
                caseType = CaseType.VALMISTUMINEN,
                yek = false
            )
        }

        val subjectCaptor = argumentCaptor<String>()
        val messageCaptor = argumentCaptor<String>()
        verify(alertPublisherService).publishAlert(subjectCaptor.capture(), messageCaptor.capture())

        assertThat(subjectCaptor.firstValue).contains("Tampere")
        assertThat(messageCaptor.firstValue).contains("SFTP connection timed out")
        assertThat(messageCaptor.firstValue).contains("localhost") // SFTP host must be in the alert
    }

    @Test
    fun `alert is published when Tampere YEK archiving fails`() {
        whenever(tampereLouhiService.laheta(any(), any()))
            .thenThrow(RuntimeException("Arkistointipalvelun hakemisto ei ole käytettävissä"))

        assertThrows(RuntimeException::class.java) {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                filePath = "/tmp/yek.zip",
                caseType = CaseType.KOEJAKSO,
                yek = true
            )
        }

        verify(alertPublisherService).publishAlert(any(), any())
    }

    @Test
    fun `no alert is published when Tampere archiving succeeds`() {
        // laheta is a void method – default mock behaviour is a no-op, which is success

        arkistointiService.laheta(
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            filePath = "/tmp/ok.zip",
            caseType = CaseType.VALMISTUMINEN,
            yek = false
        )

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    // -------------------------------------------------------------------------
    // Other universities – no alert expected
    // -------------------------------------------------------------------------

    @Test
    fun `no alert is published for universities without archiving integration`() {
        arkistointiService.laheta(
            yliopisto = YliopistoEnum.OULUN_YLIOPISTO,
            filePath = "/tmp/oulu.zip",
            caseType = CaseType.VALMISTUMINEN,
            yek = false
        )

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }
}

