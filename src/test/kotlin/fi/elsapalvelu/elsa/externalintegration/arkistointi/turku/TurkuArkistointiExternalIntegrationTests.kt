package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.Aikakonfiguraatio
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyArkistointiExternalTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepExecutor
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepLedger
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.TurkuDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [TurkuArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class TurkuArkistointiExternalIntegrationTests : DynastyArkistointiExternalTestSupport() {
    override val university = YliopistoEnum.TURUN_YLIOPISTO

    @ParameterizedTest(name = "Turku Dynasty valmistuminen {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveGraduationWithAttachments(programme: Programme) = archiveGraduation(programme)

    @ParameterizedTest(name = "Turku Dynasty koejakso {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveKoejaksoWithSecrecyMetadata(programme: Programme) = archiveKoejakso(programme)
}

@SpringBootConfiguration
@Profile("external-integration")
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    Aikakonfiguraatio::class,
    TurkuDynastyArkistointiAdapter::class,
    TurkuDynastyMetadataMapper::class,
    ArchivePayloadReader::class,
    DynastyStepExecutor::class
)
class TurkuArkistointiExternalIntegrationTestApplication {
    @Bean
    fun turkuDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient {
        val settings = properties.getArkistointi().getTurku().dynasty
        val transport = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(settings.callTimeout).readTimeout(settings.callTimeout)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .addInterceptor(DynastyTrafficLoggingInterceptor(settings))
            .build()
        return DynastyClient(settings, mapper, transport, clock)
    }

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Bean
    fun dynastyStepLedger(): DynastyStepLedger = mock()
}
