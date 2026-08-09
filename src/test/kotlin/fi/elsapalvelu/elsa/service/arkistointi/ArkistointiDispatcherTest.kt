package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ArkistointiDispatcherTest {

    @Mock
    private lateinit var configurationProvider: ArkistointiConfigurationProvider

    @Mock
    private lateinit var alertPublisherService: AlertPublisherService

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var arkistointiMetrics: ArkistointiMetricsService

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        arkistointiMetrics = ArkistointiMetricsService(meterRegistry)
    }

    @Test
    fun `enabled destination without adapter fails and records error`() {
        val request = createRequest(YliopistoEnum.TURUN_YLIOPISTO)
        whenever(configurationProvider.onKaytossa(request.yliopisto, request.caseType)).thenReturn(true)
        val dispatcher = createDispatcher(emptyList())

        assertThatIllegalStateException()
            .isThrownBy { dispatcher.laheta(request) }
            .withMessageContaining(YliopistoEnum.TURUN_YLIOPISTO.name)
            .withMessageContaining(CaseType.VALMISTUMINEN.value)

        assertThat(successCount(request)).isZero()
        assertThat(errorCount(request)).isEqualTo(1.0)
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isZero()
        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `disabled destination without adapter is ignored without recording success`() {
        val request = createRequest(YliopistoEnum.OULUN_YLIOPISTO)
        whenever(configurationProvider.onKaytossa(request.yliopisto, request.caseType)).thenReturn(false)
        val dispatcher = createDispatcher(emptyList())

        dispatcher.laheta(request)

        assertThat(successCount(request)).isZero()
        assertThat(errorCount(request)).isZero()
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isZero()
        verify(alertPublisherService, never()).publishAlert(any(), any())
    }

    @Test
    fun `duplicate adapters for same university are rejected during construction`() {
        val firstAdapter = TestAdapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        val secondAdapter = TestAdapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)

        assertThatIllegalArgumentException()
            .isThrownBy { createDispatcher(listOf(firstAdapter, secondAdapter)) }
            .withMessageContaining(YliopistoEnum.ITA_SUOMEN_YLIOPISTO.name)
    }

    @Test
    fun `success is recorded only after adapter completes`() {
        val request = createRequest(YliopistoEnum.HELSINGIN_YLIOPISTO)
        val adapter = TestAdapter(request.yliopisto) {
            assertThat(successCount(request)).isZero()
        }
        val dispatcher = createDispatcher(listOf(adapter))

        dispatcher.laheta(request)

        assertThat(adapter.receivedRequest).isSameAs(request)
        assertThat(successCount(request)).isEqualTo(1.0)
        assertThat(errorCount(request)).isZero()
    }

    @Test
    fun `adapter failure does not record success`() {
        val request = createRequest(YliopistoEnum.TURUN_YLIOPISTO)
        val adapter = TestAdapter(request.yliopisto) {
            throw RuntimeException("Dynasty ei vastaa")
        }
        val dispatcher = createDispatcher(listOf(adapter))

        org.assertj.core.api.Assertions.assertThatThrownBy { dispatcher.laheta(request) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Dynasty ei vastaa")

        assertThat(successCount(request)).isZero()
        assertThat(errorCount(request)).isEqualTo(1.0)
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isZero()
    }

    private fun createDispatcher(adapters: List<ArkistointiAdapter>) = ArkistointiDispatcher(
        adapters = adapters,
        configurationProvider = configurationProvider,
        alertPublisherService = alertPublisherService,
        arkistointiMetrics = arkistointiMetrics
    )

    private fun createRequest(yliopisto: YliopistoEnum) = ArkistointiDeliveryRequest(
        yliopisto = yliopisto,
        filePath = "/tmp/archive.zip",
        caseType = CaseType.VALMISTUMINEN,
        yek = false,
        caseId = "123",
        erikoistujanNimi = "Testi Erikoistuja"
    )

    private fun successCount(request: ArkistointiDeliveryRequest): Double =
        meterRegistry.find("arkistointi.requests.total")
            .tags("yliopisto", request.yliopisto.name, "caseType", request.caseType.value)
            .counter()?.count() ?: 0.0

    private fun errorCount(request: ArkistointiDeliveryRequest): Double =
        meterRegistry.find("arkistointi.errors.total")
            .tags("yliopisto", request.yliopisto.name, "caseType", request.caseType.value)
            .counter()?.count() ?: 0.0

    private class TestAdapter(
        override val yliopisto: YliopistoEnum,
        private val onSend: (ArkistointiDeliveryRequest) -> Unit = {}
    ) : ArkistointiAdapter {
        var receivedRequest: ArkistointiDeliveryRequest? = null
            private set

        override fun laheta(request: ArkistointiDeliveryRequest) {
            receivedRequest = request
            onSend(request)
        }
    }
}
