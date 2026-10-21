package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobFailureHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.util.function.Supplier
import javax.sql.DataSource

class ArkistointiAdapterConfigurationTest {
    private val worker = mock<Scheduler>().also { whenever(it.schedulerState).thenReturn(mock()) }
    private val dataSource = mock<DataSource>()

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `missing adapter prevents worker startup for either startup mode`(delayed: Boolean) {
        runner().withPropertyValues("db-scheduler.delay-startup-until-context-ready=$delayed").run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).hasRootCauseMessage(
                "Async archiving is enabled for HELSINGIN_YLIOPISTO / valmistuminen, " +
                    "but no asynchronous ArkistointiAdapter is registered"
            )
            verify(worker, never()).start()
            verifyNoInteractions(dataSource)
        }
    }

    @Test
    fun `matching asynchronous adapter permits worker startup`() {
        withAdapter(runner(), YliopistoEnum.HELSINGIN_YLIOPISTO).run { context ->
            assertThat(context).hasNotFailed()
            verify(worker).start()
            verifyNoInteractions(dataSource)
        }
    }

    @Test
    fun `another university adapter does not satisfy the configuration`() {
        withAdapter(runner(), YliopistoEnum.TURUN_YLIOPISTO).run { context ->
            assertThat(context).hasFailed()
            verify(worker, never()).start()
        }
    }

    @Test
    fun `duplicate adapters prevent startup`() {
        withAdapter(withAdapter(runner(), YliopistoEnum.HELSINGIN_YLIOPISTO),
            YliopistoEnum.HELSINGIN_YLIOPISTO, "secondAdapter").run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).hasRootCauseMessage(
                "Multiple archive job adapters configured for the same university"
            )
            verify(worker, never()).start()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "application.arkistointi.hki.kaytossa=false",
        "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=false"
    ])
    fun `inactive or synchronous archiving permits startup without adapters`(property: String) {
        runner().withPropertyValues(property).run { context ->
            assertThat(context).hasNotFailed()
            verify(worker).start()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["replica", "dev,replica", "dev"])
    fun `disabled profile worker allows async configuration without adapters`(profiles: String) {
        runner().withPropertyValues("spring.profiles.active=$profiles").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.environment.getProperty("db-scheduler.enabled")).isEqualTo("false")
            verify(worker, never()).start()
            verifyNoInteractions(dataSource)
        }
    }

    @Test
    fun `explicitly enabled dev worker validates adapters`() {
        runner().withPropertyValues("spring.profiles.active=dev", "db-scheduler.enabled=true").run { context ->
            assertThat(context).hasFailed()
            verify(worker, never()).start()
        }
    }

    private fun runner() = ApplicationContextRunner()
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/",
            "spring.profiles.active=adapter-validation-test",
            "db-scheduler.delay-startup-until-context-ready=false",
            "application.arkistointi.hki.kaytossa=true",
            "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=true"
        )
        .withConfiguration(AutoConfigurations.of(DbSchedulerAutoConfiguration::class.java))
        .withUserConfiguration(
            BindingConfiguration::class.java, ArkistointiDbSchedulerConfiguration::class.java,
            ArkistointiAdapterRegistry::class.java, ArkistointiConfigurationProvider::class.java
        )
        .withBean(DataSource::class.java, Supplier { dataSource })
        .withBean(Scheduler::class.java, Supplier { worker })
        .withBean(ArkistointiJobFailureHandler::class.java, Supplier { mock() })

    private fun withAdapter(
        runner: ApplicationContextRunner,
        university: YliopistoEnum,
        name: String = "testAdapter"
    ): ApplicationContextRunner = runner.withBean(name, ArkistointiAdapter::class.java, Supplier {
        mock<ArkistointiAdapter>().also { whenever(it.university).thenReturn(university) }
    })

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties::class)
    class BindingConfiguration
}
