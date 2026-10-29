package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.net.URI
import javax.cache.Caching
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider

/** Every database write targets the disposable observation-test PostgreSQL database. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveObservation?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false"
])
@Import(ArkistointiJobObservationIT.CacheConfig::class)
class ArkistointiJobObservationIT {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired private lateinit var state: ArkistointiJobStateService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var registry: MeterRegistry
    @MockitoBean private lateinit var alerts: AlertPublisherService
    @MockitoSpyBean private lateinit var observer: ArkistointiJobObserver

    @AfterEach
    fun cleanup() {
        transactions.executeWithoutResult {
            jdbc.execute("DROP TRIGGER IF EXISTS fail_observation_commit ON arkistointi_job")
            jdbc.execute("DROP FUNCTION IF EXISTS fail_observation_commit()")
            jdbc.update("delete from scheduled_tasks where task_name = ?", ARKISTOINTI_JOB_TASK.taskName)
        }
    }

    @Test
    fun `third scheduler failure warns only after the new state is committed`() {
        val execution = fixture(previousFailures = 2)
        var observedCommittedState = false
        doAnswer {
            val independent = TransactionTemplate(transactions.transactionManager!!).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }
            observedCommittedState = independent.execute {
                jdbc.queryForObject("select status from arkistointi_job where id = ?", String::class.java, execution.jobId) == "ODOTTAA"
            }
            null
        }.`when`(alerts).publishAlert(any(), any())

        assertThat(state.fail(execution, failure(), stopRetrying = false)).isTrue()

        assertThat(observedCommittedState).isTrue()
        verify(observer).onCompleted(ArkistointiAttemptCompleted(execution.jobId, YliopistoEnum.TURUN_YLIOPISTO,
            CaseType.VALMISTUMINEN, 3, ArkistointiAttemptCompleted.Outcome.RETRYABLE_FAILURE, "UNAVAILABLE"))
        verify(alerts).publishAlert(any(), any())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `failed commit emits neither metrics nor alerts`(success: Boolean) {
        val execution = fixture(previousFailures = 2)
        val count = attempts()
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_observation_commit() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN RAISE EXCEPTION 'Test commit failure'; RETURN NEW; END; ${'$'}${'$'}
            """.trimIndent())
            jdbc.execute("CREATE CONSTRAINT TRIGGER fail_observation_commit AFTER UPDATE ON arkistointi_job " +
                "DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fail_observation_commit()")
        }

        assertThrows<Exception> {
            if (success) state.succeed(execution, "external-id") else state.fail(execution, failure(), stopRetrying = true)
        }

        assertThat(jobs.findById(execution.jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(attempts()).isEqualTo(count)
        verifyNoInteractions(alerts, observer)
    }

    @Test
    fun `stale execution and repeated terminal callbacks publish no new outcomes`() {
        val execution = fixture(previousFailures = 2)
        assertThrows<StaleArkistointiExecutionException> {
            state.fail(execution.copy(version = execution.version + 1), failure(), stopRetrying = true)
        }
        verifyNoInteractions(alerts, observer)
        state.succeed(execution, "external-id")
        val count = attempts()
        state.succeed(execution, "external-id")
        state.fail(execution, failure(), stopRetrying = true)
        assertThat(attempts()).isEqualTo(count)
        verifyNoInteractions(alerts)
    }

    @Test
    fun `SNS exception cannot undo terminal commit or prevent acknowledgement`() {
        val execution = fixture(previousFailures = 0)
        doThrow(IllegalStateException("private SNS detail")).`when`(alerts).publishAlert(any(), any())

        assertThat(state.fail(execution, failure(), stopRetrying = true)).isFalse()

        assertThat(jobs.findById(execution.jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
        verify(alerts).publishAlert(any(), any())
    }

    private fun attempts() = registry.find("arkistointi.async.attempts.total").counters().sumOf { it.count() }

    private fun failure() = ArkistointiError.create("UNAVAILABLE", "private filename and payload must not reach alerts")

    private fun fixture(previousFailures: Int): ArkistointiJobExecution {
        val id = transactions.execute {
            val university = YliopistoEnum.TURUN_YLIOPISTO
            YliopistoHelper.createReferenceData(em, university)
            val source = AsiakirjaHelper.createEntity(em)
            em.persist(source)
            creator.create(CreateArkistointiJobRequest(university, CaseType.VALMISTUMINEN, "{}", "observation-${UUID.randomUUID()}",
                listOf(snapshots.createReference(source, RecordType.YHTEENVETO, 0)))).id!!
        }
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set picked = true, consecutive_failures = ? where task_instance = ?", previousFailures, id.toString())
        }
        val version = jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, id.toString())!!
        return ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, id.toString(), version)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:archive-observation-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}
