package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.impl.kayttaja.AlertPublisherServiceImpl
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest(classes = [ElsaBackendApp::class])
class RegularIntegrationContextIT {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun regularContextUsesRealRepositoryAndAlertPublisher() {
        assertThat(context.getBeansWithAnnotation(SpringBootConfiguration::class.java).keys)
            .containsExactly("elsaBackendApp")

        val repositories = context.getBeansOfType(YliopistoRepository::class.java)
        assertThat(repositories).hasSize(1)
        assertThat(mockingDetails(repositories.values.single()).isMock).isFalse()

        val publishers = context.getBeansOfType(AlertPublisherService::class.java)
        assertThat(publishers).hasSize(1)
        assertThat(publishers.values.single()).isInstanceOf(AlertPublisherServiceImpl::class.java)
        assertThat(mockingDetails(publishers.values.single()).isMock).isFalse()
    }
}
