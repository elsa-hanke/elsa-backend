package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.peppi.oulu.PeppiOuluClientBuilderImpl
import fi.elsapalvelu.elsa.service.integration.peppi.oulu.PeppiOuluOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.integration.peppi.oulu.PeppiOuluOpintotietodataFetchingServiceImpl
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiOuluExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class PeppiOuluExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var peppiOuluOpintotietodataFetchingServiceImpl: PeppiOuluOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var peppiOuluOpintosuorituksetFetchingServiceImpl: PeppiOuluOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = peppiOuluOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = peppiOuluOpintosuorituksetFetchingServiceImpl

    override val fixtureName = "peppi-oulu"

    override val expectedUniversity = YliopistoEnum.OULUN_YLIOPISTO
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
