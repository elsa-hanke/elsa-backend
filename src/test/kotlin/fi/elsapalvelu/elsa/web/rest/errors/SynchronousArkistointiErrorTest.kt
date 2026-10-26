package fi.elsapalvelu.elsa.web.rest.errors

import fi.elsapalvelu.elsa.aop.logging.LoggingAspect
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiLogCapture
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiDispatcher
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiException
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.arkistointi.siilo.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.arkistointi.siilo.SiiloArkistointiAdapter
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.boot.test.context.TestComponent
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/** Real synchronous dispatcher, Spring logging aspect and HTTP advice; only external delivery is mocked. */
class SynchronousArkistointiErrorTest {
    @ParameterizedTest
    @CsvSource(
        "HELSINGIN_YLIOPISTO, IO", "HELSINGIN_YLIOPISTO, HTTP",
        "TAMPEREEN_YLIOPISTO, IO", "TAMPEREEN_YLIOPISTO, RUNTIME"
    )
    fun `transport errors produce safe HTTP responses logs alerts and error metrics`(university: YliopistoEnum, kind: String) {
        val privateDetails = "remote-response-with-api-key-and-personal-data"
        val exception = when (kind) {
            "HTTP" -> SynchronousArkistointiException(503)
            "IO" -> IOException(privateDetails, IllegalArgumentException("nested-$privateDetails"))
            else -> IllegalStateException(privateDetails)
        }
        val siilo = mock<HelsinkiSiiloService>()
        val louhi = mock<TampereLouhiService>()
        if (university == YliopistoEnum.HELSINGIN_YLIOPISTO) {
            doAnswer { throw exception }.whenever(siilo).laheta(any(), any())
        } else {
            doAnswer { throw exception }.whenever(louhi).laheta(any(), any())
        }
        val alerts = mock<AlertPublisherService>()
        val meters = SimpleMeterRegistry()
        val metrics = ArkistointiMetricsService(meters)
        val configuration = mock<ArkistointiConfigurationProvider>()
        val dispatcher = SynchronousArkistointiDispatcher(
            listOf(SiiloArkistointiAdapter(siilo), LouhiArkistointiAdapter(louhi)), configuration, alerts, metrics
        )
        val service = SynchronousArkistointiServiceImpl(configuration, mock(), mock(), dispatcher)
        val proxy = AspectJProxyFactory(service).apply {
            addAspect(LoggingAspect(MockEnvironment().apply { setActiveProfiles("dev") }))
        }.getProxy<SynchronousArkistointiService>()
        val mvc = MockMvcBuilders.standaloneSetup(DeliveryController(proxy, university))
            .setControllerAdvice(BadRequestExceptionAdvice()).build()

        ArkistointiLogCapture().use { logs ->
            val result = mvc.perform(post("/test/archive"))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.errorKey").value(SynchronousArkistointiException.ERROR_KEY))
                .andExpect(jsonPath("$.message").value("error.${SynchronousArkistointiException.ERROR_KEY}"))
                .andExpect(jsonPath("$.detail").value(SynchronousArkistointiException.SAFE_DESCRIPTION))
                .andReturn()

            val failure = result.resolvedException as SynchronousArkistointiException
            assertThat(failure.cause).isNull()
            assertThat(failure.suppressed).isEmpty()
            assertThat(failure.remoteStatus).isEqualTo(if (kind == "HTTP") 503 else null)
            assertThat(result.response.contentAsString).doesNotContain(privateDetails, PRIVATE_PATH, PRIVATE_NAME)
            logs.assertDoesNotContain(privateDetails, PRIVATE_PATH, PRIVATE_NAME)
        }
        val alert = argumentCaptor<String>()
        verify(alerts).publishAlert(any(), alert.capture())
        assertThat(alert.firstValue).contains("Id: 42", SynchronousArkistointiException.ERROR_KEY)
            .doesNotContain(privateDetails, PRIVATE_PATH, PRIVATE_NAME)
        if (kind == "HTTP") assertThat(alert.firstValue).contains("HTTP status: 503")
        assertThat(metrics.activeArkistointiOperations.get()).isZero()
        assertThat(meters.get("arkistointi.errors.total").counter().count()).isEqualTo(1.0)
        assertThat(meters.find("arkistointi.requests.total").counter()).isNull()
        if (university == YliopistoEnum.HELSINGIN_YLIOPISTO) {
            verify(siilo).laheta(eq(PRIVATE_PATH), eq(CaseType.VALMISTUMINEN))
        } else {
            verify(louhi).laheta(eq(PRIVATE_PATH), eq(false))
        }
    }

    @RestController
    @TestComponent
    class DeliveryController(private val service: SynchronousArkistointiService, private val university: YliopistoEnum) {
        @PostMapping("/test/archive")
        fun deliver() = service.laheta(university, PRIVATE_PATH, CaseType.VALMISTUMINEN, false, "42", PRIVATE_NAME)
    }

    companion object {
        private const val PRIVATE_PATH = "/tmp/private-patient-archive.zip"
        private const val PRIVATE_NAME = "Private Patient Name"
    }
}
