package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.impl.PeppiOuluClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.PeppiOuluOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.PeppiOuluOpintotietodataFetchingServiceImpl
import org.junit.jupiter.api.Disabled
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * External integration tests for Peppi Oulu (GraphQL/Apollo).
 *
 * Tests [PeppiOuluOpintotietodataFetchingServiceImpl] and
 * [PeppiOuluOpintosuorituksetFetchingServiceImpl] against the real Oulu test endpoint.
 * The inherited [@Test] methods from [FetchingServiceExternalIntegrationBase] cover both
 * opintotietodata and opintosuoritukset in a single, consistent pass.
 */
@SpringBootTest(classes = [PeppiOuluExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
@Disabled
class PeppiOuluExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var peppiOuluOpintotietodataFetchingServiceImpl: PeppiOuluOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var peppiOuluOpintosuorituksetFetchingServiceImpl: PeppiOuluOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = peppiOuluOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = peppiOuluOpintosuorituksetFetchingServiceImpl
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    PeppiOuluClientBuilderImpl::class,
    PeppiOuluOpintotietodataFetchingServiceImpl::class,
    PeppiOuluOpintosuorituksetFetchingServiceImpl::class
)
class PeppiOuluExternalIntegrationTestApplication {
    /** Stub – [YliopistoRepository] is only used by shouldFetch* guards, not by the fetch methods under test. */
    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
