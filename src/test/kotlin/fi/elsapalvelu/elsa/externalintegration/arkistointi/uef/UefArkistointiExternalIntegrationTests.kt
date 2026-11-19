package fi.elsapalvelu.elsa.externalintegration.arkistointi.uef

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.Aikakonfiguraatio
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyArkistointiExternalTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepExecutor
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepLedger
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.UefDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.UefDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.Clock

@SpringBootTest(classes = [UefArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class UefArkistointiExternalIntegrationTests : DynastyArkistointiExternalTestSupport() {
    override val university = YliopistoEnum.ITA_SUOMEN_YLIOPISTO

    @BeforeEach
    fun requireTestArchiveOptIn() {
        assumeTrue(System.getenv("APPLICATION_ARKISTOINTI_DYNASTY_UEF_EXTERNAL_TESTS") == "true", "UEF Dynasty test archive is opt-in")
    }

    @ParameterizedTest(name = "UEF Dynasty valmistuminen {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveGraduationWithParallelDocuments(programme: Programme) = archiveGraduation(programme)

    @ParameterizedTest(name = "UEF Dynasty koejakso {0}")
    @EnumSource(value = Programme::class, names = ["EL", "EHL"])
    fun shouldArchiveKoejaksoWithSecrecyMetadata(programme: Programme) = archiveKoejakso(programme)
}

@SpringBootConfiguration
@Profile("external-integration")
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    Aikakonfiguraatio::class,
    UefDynastyArkistointiAdapter::class,
    UefDynastyMetadataMapper::class,
    ArchivePayloadReader::class,
    DynastyStepExecutor::class
)
class UefArkistointiExternalIntegrationTestApplication {
    @Bean
    fun uefDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient =
        DynastyClient(properties.getArkistointi().getUef().dynasty, mapper, clock)

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Bean
    fun dynastyStepLedger(): DynastyStepLedger = mock()
}
