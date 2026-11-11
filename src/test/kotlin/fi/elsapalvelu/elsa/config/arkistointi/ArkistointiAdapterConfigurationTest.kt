package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobFailureHandler
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobLogContext
import java.util.function.Supplier
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
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
    fun `university adapter must support every enabled case type`() {
        withAdapter(runner(), YliopistoEnum.HELSINGIN_YLIOPISTO)
            .withPropertyValues("application.arkistointi.hki.metadata.cases.koejakso.async-enabled=true").run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasRootCauseMessage(
                    "Async archiving is enabled for HELSINGIN_YLIOPISTO / koejakso, " +
                        "but the asynchronous ArkistointiAdapter does not support this case type"
                )
                verify(worker, never()).start()
                verifyNoInteractions(dataSource)
            }
    }

    @Test
    fun `adapter with both case types permits worker startup`() {
        withAdapter(runner(), YliopistoEnum.HELSINGIN_YLIOPISTO,
            supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO))
            .withPropertyValues("application.arkistointi.hki.metadata.cases.koejakso.async-enabled=true").run { context ->
                assertThat(context).hasNotFailed()
                verify(worker).start()
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

    @Test
    fun `inactive archiving permits startup without adapters`() {
        runner().withPropertyValues("application.arkistointi.hki.kaytossa=false").run { context ->
            assertThat(context).hasNotFailed()
            verify(worker).start()
        }
    }

    @Test
    fun `synchronous archiving permits startup with a synchronous adapter`() {
        runner().withPropertyValues("application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=false")
            .run { context ->
                assertThat(context).hasNotFailed()
                verify(worker).start()
            }
    }

    @ParameterizedTest
    @CsvSource(
        "turku, TURUN_YLIOPISTO, valmistuminen, true",
        "turku, TURUN_YLIOPISTO, valmistuminen, false",
        "turku, TURUN_YLIOPISTO, koejakso, true",
        "turku, TURUN_YLIOPISTO, koejakso, false",
        "uef, ITA_SUOMEN_YLIOPISTO, valmistuminen, true",
        "uef, ITA_SUOMEN_YLIOPISTO, valmistuminen, false",
        "uef, ITA_SUOMEN_YLIOPISTO, koejakso, true",
        "uef, ITA_SUOMEN_YLIOPISTO, koejakso, false"
    )
    fun `missing synchronous adapter prevents startup regardless of worker`(
        key: String, university: String, caseType: String, workerEnabled: Boolean
    ) {
        runner().withPropertyValues(
            "db-scheduler.enabled=$workerEnabled",
            "application.arkistointi.hki.kaytossa=false",
            "application.arkistointi.$key.kaytossa=true",
            "application.arkistointi.$key.metadata.cases.valmistuminen.async-enabled=true",
            "application.arkistointi.$key.metadata.cases.koejakso.async-enabled=true",
            "application.arkistointi.$key.metadata.cases.$caseType.async-enabled=false"
        ).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).hasRootCauseMessage(
                "Synchronous archiving is enabled for $university / $caseType, " +
                    "but no synchronous SynchronousArkistointiAdapter is registered; enable async archiving or register an adapter"
            )
            verify(worker, never()).start()
            verifyNoInteractions(dataSource)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["turku", "uef"])
    fun `producer may enqueue every enabled case without a local delivery adapter`(university: String) {
        runner().withPropertyValues(
            "db-scheduler.enabled=false",
            "application.arkistointi.hki.kaytossa=false",
            "application.arkistointi.$university.kaytossa=true",
            "application.arkistointi.$university.metadata.cases.valmistuminen.async-enabled=true",
            "application.arkistointi.$university.metadata.cases.koejakso.async-enabled=true"
        ).run { context ->
            assertThat(context).hasNotFailed()
            verify(worker, never()).start()
            verifyNoInteractions(dataSource)
        }
    }

    @Test
    fun `disabled university and absent case metadata require no synchronous adapter`() {
        runner().withPropertyValues(
            "application.arkistointi.hki.kaytossa=false",
            "application.arkistointi.oulu.kaytossa=true"
        ).run { context ->
            assertThat(context).hasNotFailed()
            verify(worker).start()
            verifyNoInteractions(dataSource)
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
            "spring.config.location=file:src/main/resources/config/,classpath:/config/application-archive-test.yml",
            "spring.profiles.active=adapter-validation-test",
            "db-scheduler.delay-startup-until-context-ready=false",
            "application.arkistointi.hki.kaytossa=true",
            "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=true"
        )
        .withConfiguration(AutoConfigurations.of(DbSchedulerAutoConfiguration::class.java))
        .withUserConfiguration(
            BindingConfiguration::class.java, ArkistointiDbSchedulerConfiguration::class.java,
            ArkistointiAdapterRegistry::class.java, ArkistointiConfigurationProvider::class.java,
            ArkistointiRouteValidator::class.java
        )
        .withBean(DataSource::class.java, Supplier { dataSource })
        .withBean(Scheduler::class.java, Supplier { worker })
        .withBean(ArkistointiJobFailureHandler::class.java, Supplier { mock() })
        .withBean(ArkistointiJobLogContext::class.java, Supplier { mock() })
        .withBean(SynchronousArkistointiAdapter::class.java, Supplier {
            mock<SynchronousArkistointiAdapter>().also {
                whenever(it.yliopisto).thenReturn(YliopistoEnum.HELSINGIN_YLIOPISTO)
            }
        })

    private fun withAdapter(
        runner: ApplicationContextRunner,
        university: YliopistoEnum,
        name: String = "testAdapter",
        supportedCaseTypes: Set<CaseType> = setOf(CaseType.VALMISTUMINEN)
    ): ApplicationContextRunner = runner.withBean(name, ArkistointiAdapter::class.java, Supplier {
        mock<ArkistointiAdapter>().also {
            whenever(it.university).thenReturn(university)
            whenever(it.supportedCaseTypes).thenReturn(supportedCaseTypes)
        }
    })

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties::class)
    class BindingConfiguration
}
