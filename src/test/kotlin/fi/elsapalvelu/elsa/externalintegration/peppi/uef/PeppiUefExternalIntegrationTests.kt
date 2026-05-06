package fi.elsapalvelu.elsa.externalintegration.peppi.uef

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.impl.PeppiCommonOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.PeppiCommonOpintotietodataFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.PeppiUefClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.PeppiUefOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.PeppiUefOpintotietodataFetchingServiceImpl
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * External integration tests for Peppi UEF (REST/JSON).
 *
 * Tests [PeppiUefOpintotietodataFetchingServiceImpl] and
 * [PeppiUefOpintosuorituksetFetchingServiceImpl] – including the shared
 * [PeppiCommonOpintotietodataFetchingServiceImpl] / [PeppiCommonOpintosuorituksetFetchingServiceImpl]
 * business logic – against the real UEF test endpoint.
 */
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
}

@SpringBootConfiguration
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
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
