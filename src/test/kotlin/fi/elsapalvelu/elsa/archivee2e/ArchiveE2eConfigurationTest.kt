package fi.elsapalvelu.elsa.archivee2e

import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ArchiveE2eConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = ["dev", "test", "prod", "replica", "archive-e2e,prod", "archive-e2e,replica"])
    fun `ordinary and protected profiles cannot instantiate the test adapter`(profiles: String) {
        val dataSource = mock<DataSource>()
        ApplicationContextRunner()
            .withInitializer { it.environment.setActiveProfiles(*profiles.split(",").toTypedArray()) }
            .withBean(DataSource::class.java, { dataSource })
            .withUserConfiguration(ArchiveE2eConfiguration::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean(ArchiveE2eReceipts::class.java)
                assertThat(context).doesNotHaveBean("archiveE2eHelsinkiAdapter")
                assertThat(context).doesNotHaveBean("archiveE2eUefAdapter")
                verifyNoInteractions(dataSource)
            }
    }
}
