package fi.elsapalvelu.elsa.config

import com.github.kagkarlsson.scheduler.Scheduler
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobExecution
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.cache.Caching

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        "db-scheduler.enabled=true",
        "db-scheduler.polling-interval=1h",
        "db-scheduler.threads=1"
    ]
)
@Import(DbSchedulerArkistointiJobExecutionIT.ProcessorConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DbSchedulerArkistointiJobExecutionIT {

    @Autowired
    private lateinit var scheduler: ArkistointiJobScheduler

    @Autowired
    private lateinit var backgroundScheduler: Scheduler

    @Autowired
    private lateinit var processor: RecordingProcessor

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `scheduler kutsuu processoria vasta ajastuksen commitoinnin jalkeen`() {
        val jobId = UUID.randomUUID()

        assertThat(backgroundScheduler.schedulerState.isStarted).isTrue
        transactionTemplate.executeWithoutResult {
            scheduler.schedule(jobId, Instant.now().minusSeconds(1))
            backgroundScheduler.triggerCheckForDueExecutions()

            assertNull(processor.jobIds.poll(250, TimeUnit.MILLISECONDS))
        }

        backgroundScheduler.triggerCheckForDueExecutions()
        assertThat(processor.jobIds.poll(10, TimeUnit.SECONDS)).isEqualTo(jobId)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertThat(
                jdbcTemplate.queryForObject(
                    "select count(*) from scheduled_tasks where task_name = ? and task_instance = ?",
                    Long::class.javaObjectType,
                    ARKISTOINTI_JOB_TASK.taskName,
                    jobId.toString()
                )
            ).isZero()
        }
        assertThat(processor.jobIds).isEmpty()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ProcessorConfiguration {
        @Bean
        @Primary
        fun recordingProcessor() = RecordingProcessor()

        // Closing this @DirtiesContext must not close the provider's default manager
        // still used by other cached Spring test contexts.
        @Bean
        fun isolatedJCacheManager(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:db-scheduler-execution-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }

    class RecordingProcessor : ArkistointiJobProcessor {
        val jobIds = LinkedBlockingQueue<UUID>()

        override fun process(execution: ArkistointiJobExecution) {
            jobIds.add(execution.jobId)
        }
    }
}
