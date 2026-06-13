package fi.elsapalvelu.elsa.externalintegration.peppi.uef

import com.fasterxml.jackson.databind.DeserializationFeature
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintotietodataFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.uef.PeppiUefClientBuilderImpl
import fi.elsapalvelu.elsa.service.integration.peppi.uef.PeppiUefOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.uef.PeppiUefOpintotietodataFetchingServiceImpl
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

@SpringBootTest(classes = [PeppiUefExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class PeppiUefExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var peppiUefOpintotietodataFetchingServiceImpl: PeppiUefOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var peppiUefOpintosuorituksetFetchingServiceImpl: PeppiUefOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = peppiUefOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = peppiUefOpintosuorituksetFetchingServiceImpl

    override val assertErikoisalaTunnisteList: Boolean = false

    override fun getTestHetu() = "130560-9972"
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    PeppiUefClientBuilderImpl::class,
    PeppiCommonOpintotietodataFetchingServiceImpl::class,
    PeppiCommonOpintosuorituksetFetchingServiceImpl::class,
    PeppiUefOpintotietodataFetchingServiceImpl::class,
    PeppiUefOpintosuorituksetFetchingServiceImpl::class
)
class PeppiUefExternalIntegrationTestApplication {
    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer {
            it.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }

    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
