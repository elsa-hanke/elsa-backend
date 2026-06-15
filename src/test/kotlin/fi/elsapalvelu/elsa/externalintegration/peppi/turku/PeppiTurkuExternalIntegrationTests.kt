package fi.elsapalvelu.elsa.externalintegration.peppi.turku

import com.fasterxml.jackson.databind.DeserializationFeature
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintotietodataFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.turku.PeppiTurkuClientBuilderImpl
import fi.elsapalvelu.elsa.service.integration.peppi.turku.PeppiTurkuOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.turku.PeppiTurkuOpintotietodataFetchingServiceImpl
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiTurkuExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class PeppiTurkuExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var peppiTurkuOpintotietodataFetchingServiceImpl: PeppiTurkuOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var peppiTurkuOpintosuorituksetFetchingServiceImpl: PeppiTurkuOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = peppiTurkuOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = peppiTurkuOpintosuorituksetFetchingServiceImpl

    override fun getTestHetu() = "010957-994N"
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    PeppiTurkuClientBuilderImpl::class,
    PeppiCommonOpintotietodataFetchingServiceImpl::class,
    PeppiCommonOpintosuorituksetFetchingServiceImpl::class,
    PeppiTurkuOpintotietodataFetchingServiceImpl::class,
    PeppiTurkuOpintosuorituksetFetchingServiceImpl::class
)
class PeppiTurkuExternalIntegrationTestApplication {
    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer {
            it.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }

    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
