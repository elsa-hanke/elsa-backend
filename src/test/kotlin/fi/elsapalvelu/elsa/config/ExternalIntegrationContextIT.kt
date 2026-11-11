package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.externalintegration.peppi.oulu.PeppiOuluExternalIntegrationTestApplication
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.peppi.oulu.PeppiOuluOpintotietodataFetchingServiceImpl
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@Tag("external-integration")
@SpringBootTest(classes = [PeppiOuluExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class ExternalIntegrationContextIT {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun externalContextKeepsRealClientAndMockedInfrastructure() {
        assertThat(context.getBeansOfType(PeppiOuluOpintotietodataFetchingServiceImpl::class.java)).hasSize(1)
        assertThat(context.getBeansOfType(DataSource::class.java)).isEmpty()
        assertThat(mockingDetails(context.getBean(YliopistoRepository::class.java)).isMock).isTrue()
        assertThat(mockingDetails(context.getBean(AlertPublisherService::class.java)).isMock).isTrue()
    }
}
