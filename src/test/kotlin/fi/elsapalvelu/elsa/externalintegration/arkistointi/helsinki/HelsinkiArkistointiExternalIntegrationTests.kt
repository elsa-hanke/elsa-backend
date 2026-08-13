package fi.elsapalvelu.elsa.externalintegration.arkistointi.helsinki

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiDispatcher
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestData
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestScenario
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.arkistointi.siilo.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.arkistointi.siilo.SiiloArkistointiAdapter
import fi.elsapalvelu.elsa.service.impl.kayttaja.AlertPublisherServiceImpl
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Paths
import java.util.stream.Stream

/**
 * Sends a real SÄHKE2 package to every Helsinki Siilo test collection used by ELSA:
 * valmistuminen (15), sopimus (16), and koejakso (17).
 */
@SpringBootTest(classes = [HelsinkiArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class HelsinkiArkistointiExternalIntegrationTests : ExternalIntegrationTestSupport() {

    @Autowired
    private lateinit var arkistointiService: ArkistointiServiceImpl

    @ParameterizedTest(name = "{0}")
    @MethodSource("deliveryScenarios")
    fun shouldLahetaSahkeToSiiloWithoutErrors(scenario: ArkistointiTestScenario) {
        val input = ArkistointiTestData.createInput(scenario)
        val result = ArkistointiTestData.createPackage(arkistointiService, input)

        log.info(
            "Sending Helsinki Siilo archive test: scenario={}, correlationId={}, zip={}",
            scenario.name,
            input.correlationId,
            result.zipFilePath
        )

        assertThatCode {
            arkistointiService.laheta(
                yliopisto = scenario.yliopisto,
                filePath = result.zipFilePath,
                caseType = scenario.caseType,
                yek = scenario.yek,
                caseId = input.caseId,
                erikoistujanNimi = "ELSA integraatiotesti ${input.correlationId}"
            )
        }
            .describedAs("Siilo delivery must succeed for ${scenario.name}")
            .doesNotThrowAnyException()

        assertThat(Paths.get(result.zipFilePath))
            .describedAs("Siilo delivery must delete the local temporary ZIP")
            .doesNotExist()
    }

    companion object {
        @JvmStatic
        fun deliveryScenarios(): Stream<ArkistointiTestScenario> =
            ArkistointiTestData.helsinkiScenarios.stream()
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    HelsinkiSiiloService::class,
    LouhiArkistointiAdapter::class,
    SiiloArkistointiAdapter::class,
    ArkistointiConfigurationProvider::class,
    SahkeMetadataBuilder::class,
    SahkePakettiBuilder::class,
    ArkistointiDispatcher::class,
    ArkistointiServiceImpl::class,
    ArkistointiMetricsService::class,
    AlertPublisherServiceImpl::class
)
class HelsinkiArkistointiExternalIntegrationTestApplication {

    @Bean
    fun tampereLouhiService(): TampereLouhiService =
        Mockito.mock(TampereLouhiService::class.java)

    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
