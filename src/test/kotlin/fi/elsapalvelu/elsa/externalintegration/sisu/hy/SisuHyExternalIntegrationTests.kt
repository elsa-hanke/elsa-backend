package fi.elsapalvelu.elsa.externalintegration.sisu.hy

import com.fasterxml.jackson.databind.DeserializationFeature
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaFetchingService
import fi.elsapalvelu.elsa.service.impl.SisuHyClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.SisuHyOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.SisuHyOpintotietodataFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.SisuTutkintoohjelmaFetchingServiceImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * External integration tests for Sisu HY (GraphQL/Apollo + REST export).
 *
 * Tests [SisuHyOpintotietodataFetchingServiceImpl], [SisuHyOpintosuorituksetFetchingServiceImpl],
 * and [SisuTutkintoohjelmaFetchingServiceImpl] against the real HY test endpoint.
 *
 * The inherited [@Test] methods from [FetchingServiceExternalIntegrationBase] cover opintotietodata
 * and opintosuoritukset; [shouldFetchTutkintoohjelmatWithoutErrors] adds coverage for the
 * qualification-export endpoint used by the scheduled Sisu programme import.
 */
@SpringBootTest(classes = [SisuHyExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
@Disabled
class SisuHyExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var sisuHyOpintotietodataFetchingServiceImpl: SisuHyOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var sisuHyOpintosuorituksetFetchingServiceImpl: SisuHyOpintosuorituksetFetchingServiceImpl

    @Autowired
    private lateinit var sisuHyClientBuilderImpl: SisuHyClientBuilderImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = sisuHyOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = sisuHyOpintosuorituksetFetchingServiceImpl

    @Autowired
    private lateinit var sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService

    @Test
    fun shouldBuildApolloClientWithoutRuntimeLinkageErrors() {
        assertThatCode { sisuHyClientBuilderImpl.apolloClient() }
            .describedAs("Apollo client creation must not fail because of incompatible runtime dependencies")
            .doesNotThrowAnyException()
    }

    @Test
    fun shouldFetchTutkintoohjelmatWithoutErrors() {
        val result = runBlocking { sisuTutkintoohjelmaFetchingService.fetch() }

        assertThat(result)
            .describedAs("fetch() must not return null (indicates network/parsing failure)")
            .isNotNull
        assertThat(result!!.entities)
            .describedAs("Qualifications entities must not be null or empty")
            .isNotEmpty
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@ImportAutoConfiguration(JacksonAutoConfiguration::class)
@Import(
    SisuHyClientBuilderImpl::class,
    SisuHyOpintotietodataFetchingServiceImpl::class,
    SisuHyOpintosuorituksetFetchingServiceImpl::class,
    SisuTutkintoohjelmaFetchingServiceImpl::class
)
class SisuHyExternalIntegrationTestApplication {
    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer {
            it.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }

    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
