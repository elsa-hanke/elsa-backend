package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerStarter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier
import javax.sql.DataSource

class ArkistointiSchedulerProfileTest {
    private val dataSource = mock<DataSource>()

    @ParameterizedTest
    @ValueSource(strings = ["replica", "dev,replica", "dev"])
    fun `local and replica profiles do not create or start a database worker`(profiles: String) {
        runner(profiles).run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.environment.activeProfiles).containsAll(profiles.split(","))
            assertThat(context.environment.getProperty("db-scheduler.enabled")).isEqualTo("false")
            assertThat(context).doesNotHaveBean(Scheduler::class.java)
            assertThat(context).doesNotHaveBean(DbSchedulerStarter::class.java)
            verifyNoInteractions(dataSource)
        }
    }

    @Test
    fun `dev worker can be explicitly enabled`() {
        val worker = mock<Scheduler>()
        whenever(worker.schedulerState).thenReturn(mock())
        runner("dev")
            .withPropertyValues("db-scheduler.enabled=true", "db-scheduler.delay-startup-until-context-ready=false")
            .withBean(Scheduler::class.java, Supplier { worker })
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(DbSchedulerStarter::class.java)
                verify(worker).start()
                verifyNoInteractions(dataSource)
            }
    }

    private fun runner(profiles: String) = ApplicationContextRunner()
        // Load production resources explicitly: test application.yml already disables the worker
        // and would otherwise mask a missing safeguard in the real profile configuration.
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/",
            "spring.profiles.active=$profiles"
        )
        .withConfiguration(AutoConfigurations.of(DbSchedulerAutoConfiguration::class.java))
        // Never connect to a real replica, even when this regression test runs before the fix.
        .withBean(DataSource::class.java, Supplier { dataSource })
}
