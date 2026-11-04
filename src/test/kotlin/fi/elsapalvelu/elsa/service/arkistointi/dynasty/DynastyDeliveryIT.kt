package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveSchedulerTestControl
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.cache.CacheManager
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status



/** Real approval, PostgreSQL snapshots, production scheduler, adapter, ledger and HTTP client. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
 "spring.datasource.url=jdbc:tc:postgresql:16.9:///dynastyDelivery?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
 "application.arkistointi.turku.kaytossa=true",
 "db-scheduler.heartbeat-interval=1h",
 "application.arkistointi.turku.metadata.cases.koejakso.async-enabled=true",
 "application.arkistointi.turku.metadata.cases.koejakso.documents.arviointi.retention-period=-1",
 "application.arkistointi.turku.dynasty.client-id=test",
 "application.arkistointi.turku.dynasty.client-secret=test",
 "application.arkistointi.turku.dynasty.organization=0",
 "application.arkistointi.turku.dynasty.case-schema-id=case",
 "application.arkistointi.turku.dynasty.document-schema-id=document",
 "application.arkistointi.turku.dynasty.allow-loopback-http-for-tests=true",
 "application.arkistointi.turku.dynasty.tutkintoasetukset[test]=Test regulation",
 "application.arkistointi.turku.dynasty.koejakso-publicity-class-id=test-secret",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[reason]=test-law",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[duration]=100",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[start]=approval"
])
@Import(DynastyDeliveryIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyDeliveryIT : AsyncArchiveItSupport() {





 @Autowired private lateinit var properties: fi.elsapalvelu.elsa.config.ApplicationProperties
 @Autowired private lateinit var client: DynastyClient
 @Autowired private lateinit var task: com.github.kagkarlsson.scheduler.task.helper.OneTimeTask<Unit>
 @Autowired private lateinit var dataSource: javax.sql.DataSource
 @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
 @MockitoBean lateinit var mail: MailService
    private lateinit var schedulerControl: ArchiveSchedulerTestControl

    @BeforeEach
    fun resetReceiver() {
        ReflectionTestUtils.setField(client, "token", null)
        schedulerControl = ArchiveSchedulerTestControl(scheduler, processor)
        pollAndAwait()

        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Remove requests left by a failed previous assertion.
        }
    }

    @AfterEach
    fun clearPendingExecutions() {
        server.dispatcher = QueueDispatcher()
        server.enqueue(MockResponse().setResponseCode(400))
        schedulerControl.releasePendingAttempts()
        await().atMost(Duration.ofSeconds(30)).until { scheduler.currentlyExecuting.isEmpty() }
        jdbc.queryForList("select task_instance from scheduled_tasks where task_name = 'arkistointi-job'", String::class.java)
            .forEach { scheduler.cancel(TaskInstanceId.of("arkistointi-job", it)) }
    }


    private fun job(): fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        return jobs.findByKey("koejakso-arvio:TURUN_YLIOPISTO:$id")!!
    }
    private fun enqueue(vararg bodies: String) = bodies.forEach { server.enqueue(MockResponse().setBody(it)) }
    @Test fun `scheduler delivers through real adapter and commits every ledger step`() {
        val job = job()
        enqueue(TOKEN_RESPONSE, "415", "\"${java.util.UUID.randomUUID()}\"", "1078", "")
        startAttempt()
        awaitDelivered(job.id!!)
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        assertThat(jdbc.queryForList("select status from arkistointi_dynasty_step where job_id=?", String::class.java, job.id))
            .containsExactlyInAnyOrder("VALMIS","VALMIS","VALMIS","VALMIS")
    }
    @Test fun `success commit failure resumes completed ledger without another HTTP request`() {
        val job = job()
        withSuccessCommitFailure {
            enqueue(TOKEN_RESPONSE, "415", "\"${java.util.UUID.randomUUID()}\"", "1078", "")
            startAttempt()
            await().atMost(Duration.ofSeconds(30)).untilAsserted {
                assertThat(scheduler.currentlyExecuting).isEmpty()
                val stored = jobs.findById(job.id!!).orElseThrow()
                assertThat(stored.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
                assertThat(stored.errorCode).isEqualTo("ARCHIVE_SUCCESS_PERSISTENCE_FAILED")
            }
        }
        val requests = server.requestCount
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set execution_time=clock_timestamp() " +
                "where task_name='arkistointi-job' and task_instance=? and picked=false", job.id.toString())
        }
        startAttempt()
        awaitDelivered(job.id!!)
        assertThat(server.requestCount).isEqualTo(requests)
    }

    @Test fun `manual retry skips completed case and task after definite document rejection`() {
        val job = job()
        enqueue(TOKEN_RESPONSE, "415", "\"${java.util.UUID.randomUUID()}\"")
        server.enqueue(MockResponse().setResponseCode(403))
        startAttempt()
        awaitParked(job.id!!)
        assertThat(jdbc.queryForList("select step_key from arkistointi_dynasty_step where job_id=? and status='VALMIS'", String::class.java,job.id))
            .containsExactlyInAnyOrder("CASE","TASK")
        retryParkedJob(job.id!!)
        enqueue("1078", "")
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val result = jobs.findById(job.id!!).orElseThrow()
            assertThat(result.status)
                .describedAs("error=%s steps=%s",result.errorCode,jdbc.queryForList("select step_key,status,remote_id from arkistointi_dynasty_step where job_id=?",job.id))
                .isEqualTo(ArkistointiJobStatus.LAHETETTY)
        }
        val requests = (1..6).map { server.takeRequest(5, TimeUnit.SECONDS)!! }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        assertThat(requests.count { it.path!!.contains("/tasks") }).isEqualTo(1)
    }
    @Test fun `lost case response parks job and retry never creates another case`() {
        val job = job()
        enqueue(TOKEN_RESPONSE)
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
        startAttempt()
        awaitParked(job.id!!)
        val before = server.requestCount
        retryParkedJob(job.id!!)
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val pending = jobs.findById(job.id!!).orElseThrow()
            assertThat(pending.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(pending.errorCode).isEqualTo("DYNASTY_STEP_IN_PROGRESS")
            assertThat(jdbc.queryForObject("select picked from scheduled_tasks where task_instance=?", Boolean::class.javaObjectType, job.id.toString())).isFalse()
        }
        assertThat(server.requestCount).isEqualTo(before)
        assertThat(jdbc.queryForObject("select status from arkistointi_dynasty_step where job_id=? and step_key='CASE'", String::class.java, job.id)).isEqualTo("ALOITETTU")
    }
    @Test fun `lost heartbeat during HTTP send parks job and late case response is recorded without another step`() {
        val job = job()
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val requestsBefore = server.requestCount
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/token") return MockResponse().setBody(TOKEN_RESPONSE)
                entered.countDown()
                check(release.await(30, TimeUnit.SECONDS))
                return MockResponse().setBody("415")
            }
        }
        val recoveryDataSource = object : org.springframework.jdbc.datasource.DelegatingDataSource(dataSource) {
            override fun getConnection(): java.sql.Connection = super.getConnection().apply { autoCommit = true }
        }
        val recovery = com.github.kagkarlsson.scheduler.Scheduler.create(recoveryDataSource, task)
            .schedulerName(com.github.kagkarlsson.scheduler.SchedulerName.Fixed("dynasty-recovery-it"))
            .threads(1).pollingInterval(Duration.ofHours(1)).heartbeatInterval(Duration.ofSeconds(1))
            .missedHeartbeatsLimit(4).shutdownMaxWait(Duration.ofSeconds(3)).build()
        try {
            startAttempt()
            assertThat(entered.await(20, TimeUnit.SECONDS)).isTrue()
            val version = jdbc.queryForObject("select version from scheduled_tasks where task_instance=?", Long::class.javaObjectType, job.id.toString())!!
            // A second real scheduler detects the missing heartbeat while the HTTP request remains blocked.
            recovery.start()
            await().atMost(Duration.ofSeconds(15)).untilAsserted {
                val stored = jobs.findById(job.id!!).orElseThrow()
                assertThat(stored.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
                assertThat(stored.errorCode).isEqualTo("ARCHIVE_EXECUTION_LOST")
                assertThat(jdbc.queryForObject("select picked from scheduled_tasks where task_instance=?",
                    Boolean::class.javaObjectType, job.id.toString())).isFalse()
            }
            assertThat(jdbc.queryForObject("select execution_version from arkistointi_dynasty_step where job_id=? and step_key='CASE'",Long::class.javaObjectType,job.id))
                .isEqualTo(version)
            release.countDown()
            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                assertThat(jdbc.queryForMap("select status,remote_id from arkistointi_dynasty_step where job_id=? and step_key='CASE'",job.id))
                    .containsEntry("status","VALMIS").containsEntry("remote_id","415")
                assertThat(scheduler.currentlyExecuting).isEmpty()
            }
            assertThat(server.requestCount).isEqualTo(requestsBefore + 2)
            assertThat(jdbc.queryForObject("select count(*) from arkistointi_dynasty_step where job_id=? and step_key='TASK'",Int::class.javaObjectType,job.id)).isZero()
        } finally {
            release.countDown()
            recovery.stop()
        }
    }
    private fun approveKoejakso(type: CaseType): Long {
        val fixture = koejaksoFixture(YliopistoEnum.TURUN_YLIOPISTO, type)
        transactions.executeWithoutResult {
            val studyRight = assessments.findById(fixture.second).orElseThrow().opintooikeus!!
            properties.getArkistointi().getTurku().dynasty.tutkintoasetukset = mapOf(studyRight.asetus!!.nimi!! to "Test regulation")
        }
        val result = if (type == CaseType.SOPIMUS) approveContract(fixture) else approveAssessment(fixture)
        result.andExpect(status().isOk)
        return if (type == CaseType.SOPIMUS) fixture.first else fixture.second
    }

    private fun startAttempt() = schedulerControl.startAttempt()

    private fun pollAndAwait() = schedulerControl.pollAndAwait()

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:dynasty-delivery-it", javaClass.classLoader, customizer)
    }

    companion object {
        private const val TOKEN_RESPONSE = """{"access_token":"test","expires_in":900}"""
        private val server = MockWebServer().apply { start() }
        @JvmStatic @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            registry.add("application.arkistointi.turku.dynasty.base-url") { server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/') }
            registry.add("application.arkistointi.turku.dynasty.token-url") { server.url("/token").newBuilder().host("127.0.0.1").build().toString() }
        }
        @JvmStatic @AfterAll fun stopServer() = server.shutdown()
    }
}
