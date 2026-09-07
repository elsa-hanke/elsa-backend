package fi.elsapalvelu.elsa.config

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

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

    @Test
    fun `job ajastetaan db scheduleriin vain kerran`() {
        val jobId = seuraavaJobId()
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
        val jobId = seuraavaJobId()

        transactionTemplate.executeWithoutResult {
            scheduler.schedule(jobId, Instant.parse("2099-01-01T00:00:00Z"))
            it.setRollbackOnly()
        }

        assertThat(scheduledTaskCount(jobId)).isZero
    }

    @Test
    fun `jobia ei voi ajastaa ilman transaktiota`() {
        assertThrows<IllegalTransactionStateException> {
            scheduler.schedule(seuraavaJobId())
        }
    }

    @Test
    fun `tausta scheduler kayttaa automaattisesti commitoivaa yhteytta`() {
        dbSchedulerCustomizer.dataSource().orElseThrow().connection.use {
            assertThat(it.autoCommit).isTrue
        }
    }

    private fun scheduledTaskCount(jobId: Long): Long =
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

    companion object {
        private val jobIds = AtomicLong(System.currentTimeMillis())

        private fun seuraavaJobId() = jobIds.incrementAndGet()
    }
}
