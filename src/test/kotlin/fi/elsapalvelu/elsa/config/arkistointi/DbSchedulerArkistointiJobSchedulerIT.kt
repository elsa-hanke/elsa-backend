package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        "db-scheduler.enabled=true",
        "db-scheduler.polling-interval=1h",
        "db-scheduler.threads=1"
    ]
)
class DbSchedulerArkistointiJobSchedulerIT {

    @Autowired
    private lateinit var scheduler: ArkistointiJobScheduler

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var dbSchedulerCustomizer: DbSchedulerCustomizer

    @Autowired
    private lateinit var backgroundScheduler: Scheduler

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `scheduler kaynnistyy Spring kontekstin mukana`() {
        assertThat(backgroundScheduler.schedulerState.isStarted).isTrue
    }

    @Test
    fun `scheduler sammuu Spring kontekstin sulkeutuessa`() {
        val runningScheduler = AtomicReference<Scheduler>()
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DbSchedulerAutoConfiguration::class.java))
            .withUserConfiguration(ArkistointiDbSchedulerConfiguration::class.java)
            // The wrapper prevents closing the parent context's shared connection pool.
            .withBean(DataSource::class.java, Supplier { DelegatingDataSource(dataSource) })
            .withPropertyValues(
                "db-scheduler.enabled=true",
                "db-scheduler.delay-startup-until-context-ready=true",
                "db-scheduler.polling-interval=1h",
                "db-scheduler.threads=1"
            )
            .run { context ->
                assertThat(context).hasNotFailed()
                val scheduler = context.getBean(Scheduler::class.java)
                assertThat(scheduler.schedulerState.isStarted).isTrue
                runningScheduler.set(scheduler)
            }

        assertThat(runningScheduler.get().schedulerState.isShuttingDown).isTrue
    }

    @Test
    fun `puuttuva processor ei paata suoritusta onnistuneena`() {
        val jobId = UUID.randomUUID()
        try {
            transactionTemplate.executeWithoutResult {
                scheduler.schedule(jobId, Instant.now().minusSeconds(1))
            }
            backgroundScheduler.triggerCheckForDueExecutions()

            await().atMost(Duration.ofSeconds(10)).untilAsserted {
                val execution = jdbcTemplate.queryForMap(
                    "select * from scheduled_tasks where task_name = ? and task_instance = ?",
                    ARKISTOINTI_JOB_TASK.taskName,
                    jobId.toString()
                )
                assertThat(execution["consecutive_failures"]).isEqualTo(1)
                assertThat(execution["last_failure"]).isNotNull
                assertThat(execution["last_success"]).isNull()
                assertThat(execution["picked"]).isEqualTo(false)
            }
        } finally {
            transactionTemplate.executeWithoutResult {
                jdbcTemplate.update(
                    "delete from scheduled_tasks where task_name = ? and task_instance = ?",
                    ARKISTOINTI_JOB_TASK.taskName,
                    jobId.toString()
                )
            }
        }
    }

    @Test
    fun `job ajastetaan db scheduleriin vain kerran`() {
        val jobId = UUID.randomUUID()
        val suoritusAika = Instant.parse("2099-01-01T00:00:00Z")

        transactionTemplate.executeWithoutResult {
            assertThat(scheduler.schedule(jobId, suoritusAika)).isTrue
            assertThat(scheduler.schedule(jobId, suoritusAika)).isFalse
        }

        assertThat(scheduledTaskCount(jobId)).isEqualTo(1)
        val taskDataIsNull = jdbcTemplate.queryForObject(
            """
            select task_data is null
            from scheduled_tasks
            where task_name = ? and task_instance = ?
            """.trimIndent(),
            Boolean::class.javaObjectType,
            ARKISTOINTI_JOB_TASK.taskName,
            jobId.toString()
        ) ?: false

        assertThat(taskDataIsNull).isTrue
    }

    @Test
    fun `ajastus peruuntuu ymparoivan transaktion mukana`() {
        val jobId = UUID.randomUUID()

        transactionTemplate.executeWithoutResult {
            scheduler.schedule(jobId, Instant.parse("2099-01-01T00:00:00Z"))
            it.setRollbackOnly()
        }

        assertThat(scheduledTaskCount(jobId)).isZero
    }

    @Test
    fun `jobia ei voi ajastaa ilman transaktiota`() {
        assertThrows<IllegalTransactionStateException> {
            scheduler.schedule(UUID.randomUUID())
        }
    }

    @Test
    fun `tausta scheduler kayttaa automaattisesti commitoivaa yhteytta`() {
        dbSchedulerCustomizer.dataSource().orElseThrow().connection.use {
            assertThat(it.autoCommit).isTrue
        }
    }

    private fun scheduledTaskCount(jobId: UUID): Long =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from scheduled_tasks
            where task_name = ? and task_instance = ?
            """.trimIndent(),
            Long::class.javaObjectType,
            ARKISTOINTI_JOB_TASK.taskName,
            jobId.toString()
        ) ?: 0

}
