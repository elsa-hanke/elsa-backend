package fi.elsapalvelu.elsa.externalintegration.arkistointi.uef

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyArkistointiExternalTestSupport
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyExternalTestConfiguration
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.externalDynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDestination
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.UefDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.UefDynastyMetadataMapper
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.SpringBootConfiguration
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
    @EnumSource(Programme::class)
    fun shouldArchiveKoejaksoWithSecrecyMetadata(programme: Programme) = archiveKoejakso(programme)
}

@SpringBootConfiguration
@Profile("external-integration")
@Import(DynastyExternalTestConfiguration::class, UefDynastyArkistointiAdapter::class, UefDynastyMetadataMapper::class)
class UefArkistointiExternalIntegrationTestApplication {
    @Bean
    fun uefDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient {
        val settings = DynastyDestination.UEF.archiveProperties(properties.getArkistointi()).dynasty
        return externalDynastyClient(settings, mapper, clock)
    }
}
