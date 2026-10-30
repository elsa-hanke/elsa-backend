package fi.elsapalvelu.elsa.service.arkistointi.siilo

import com.github.kagkarlsson.scheduler.event.AbstractSchedulerListener
import com.github.kagkarlsson.scheduler.event.SchedulerListener.SchedulerEventType
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Semaphore
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import java.time.Instant
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import com.github.kagkarlsson.scheduler.Scheduler
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.cache.Caching

import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/** Real HTTP approval, PDFs, immutable snapshots, PostgreSQL, scheduler and Siilo HTTP multipart. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///helsinkiAsyncApproval?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.hki.kaytossa=true",
    "application.arkistointi.tre.kaytossa=false",
    "application.arkistointi.hki.api-key=integration-test",
    "application.arkistointi.hki.async-allow-loopback-http-for-tests=true",
    "application.arkistointi.hki.metadata.zip-metadata=true",
    "application.arkistointi.hki.metadata.organisation=Helsingin yliopisto",
    "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.valmistuminen.title=Valmistuminen",
    "application.arkistointi.hki.metadata.cases.valmistuminen.type=Raportti",
    "application.arkistointi.hki.metadata.cases.valmistuminen.siilo-koodi=graduation",
    "application.arkistointi.hki.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.hki.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.koejakso.title=Koejakson arviointi",
    "application.arkistointi.hki.metadata.cases.koejakso.type=Muu",
    "application.arkistointi.hki.metadata.cases.koejakso.siilo-koodi=assessment",
    "application.arkistointi.hki.metadata.cases.koejakso.documents.arviointi.retention-period=20",
    "application.arkistointi.hki.metadata.cases.sopimus.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.sopimus.title=Koulutuspaikkasopimus",
    "application.arkistointi.hki.metadata.cases.sopimus.type=Sopimus",
    "application.arkistointi.hki.metadata.cases.sopimus.siilo-koodi=contract",
    "application.arkistointi.hki.metadata.cases.sopimus.documents.sopimus.retention-period=20",
    "application.arkistointi.retry.initial-delay=3s",
    "application.arkistointi.retry.max-delay=3s",
    "application.arkistointi.retry.jitter-factor=0"
])
@Import(HelsinkiAsyncApprovalIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HelsinkiAsyncApprovalIT : AsyncArchiveItSupport() {
    @Autowired private lateinit var scheduler: Scheduler
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper
    @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
    @MockitoBean lateinit var mail: MailService

    private val pollCount = AtomicInteger()
    private val attemptPermits = Semaphore(0)

    @BeforeEach
    fun resetReceiver() {
        scheduler.registerSchedulerListener(object : AbstractSchedulerListener() {
            override fun onSchedulerEvent(type: SchedulerEventType) {
                if (type == SchedulerEventType.RAN_EXECUTE_DUE) pollCount.incrementAndGet()
            }
        })
        // A poll may pick a task, but only the test may let it enter the real processor.
        doAnswer { invocation ->
            check(attemptPermits.tryAcquire(30, TimeUnit.SECONDS)) { "Test did not authorize archive attempt" }
            invocation.callRealMethod()
        }.whenever(processor).process(any<ArkistointiJobExecution>())
        pollAndAwait()

        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Remove requests left by a failed previous assertion.
        }
    }

    @AfterEach
    fun clearPendingExecutions() {
        server.enqueue(MockResponse().setResponseCode(400))
        attemptPermits.release(10)
        await().atMost(Duration.ofSeconds(30)).until { scheduler.currentlyExecuting.isEmpty() }
        jdbc.queryForList("select task_instance from scheduled_tasks where task_name = 'arkistointi-job'", String::class.java)
            .forEach { scheduler.cancel(TaskInstanceId.of("arkistointi-job", it)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation approval delivers only Helsinki configured documents`(kind: String) {
        val fixture = graduationFixture(kind, YliopistoEnum.HELSINGIN_YLIOPISTO, true)
        setSecurityContext(fixture.second, VASTUUHENKILO)
        testMockMvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${fixture.first}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null))).with(csrf()))
            .andExpect(status().isOk)
        assertDelivery("valmistuminen:HELSINGIN_YLIOPISTO:${fixture.first}", RecordType.YHTEENVETO, "graduation")
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `koejakso approval delivers its saved contract or assessment`(type: CaseType) {
        val id = approveKoejakso(type)
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        assertDelivery("$prefix:HELSINGIN_YLIOPISTO:$id",
            if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI,
            if (type == CaseType.SOPIMUS) "contract" else "assessment")
    }

    @Test
    fun `temporary receiver outage preserves committed approval and retries the same ZIP`() {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val job = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!
        server.enqueue(MockResponse().setResponseCode(503).setBody("private response"))
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jdbc.queryForObject("select consecutive_failures from scheduled_tasks where task_instance = ?",
                Int::class.javaObjectType, job.id.toString())).isEqualTo(1)
        }
        pollAndAwait()
        assertThat(jobs.findById(job.id!!).orElseThrow().status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(assessments.findById(id).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
        val first = zipEntries(receivedZip(server.takeRequest(5, TimeUnit.SECONDS)!!))
        server.enqueue(MockResponse().setResponseCode(200))
        val nextExecution = jdbc.queryForObject(
            "select execution_time from scheduled_tasks where task_instance = ?",
            java.time.OffsetDateTime::class.java, job.id.toString()
        )!!.toInstant()
        // Let the library's saved retry become due; never rewrite scheduler state with SQL.
        await().atMost(Duration.ofSeconds(10)).until { !Instant.now().isBefore(nextExecution) }
        startAttempt()
        awaitDelivered(job.id!!)
        val retried = zipEntries(receivedZip(server.takeRequest(5, TimeUnit.SECONDS)!!))
        assertThat(retried.keys).isEqualTo(first.keys)
        first.forEach { (name, bytes) -> assertThat(retried.getValue(name)).isEqualTo(bytes) }
    }

    @Test
    fun `permanent receiver rejection ends retries without rolling back approval`() {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val job = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!
        server.enqueue(MockResponse().setResponseCode(400).setBody("private response"))
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jobs.findById(job.id!!).orElseThrow().status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
            assertThat(taskCount(job.id!!)).isZero()
        }
        assertThat(assessments.findById(id).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
        server.takeRequest(5, TimeUnit.SECONDS)!!
    }

    private fun approveKoejakso(type: CaseType): Long {
        val fixture = koejaksoFixture(YliopistoEnum.HELSINGIN_YLIOPISTO, type)
        setSecurityContext(fixture.third, VASTUUHENKILO)
        val id = if (type == CaseType.SOPIMUS) fixture.first else fixture.second
        val dto = transactions.execute { _ ->
            if (type == CaseType.SOPIMUS) contractMapper.toDto(contracts.findById(id).orElseThrow())
            else assessmentMapper.toDto(assessments.findById(id).orElseThrow()).apply { koejaksoHyvaksytty = true }
        }
        val path = if (type == CaseType.SOPIMUS) "koulutussopimus" else "vastuuhenkilonarvio"
        testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/$path")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
            .andExpect(status().isOk)
        return id
    }

    private fun assertDelivery(key: String, type: RecordType, code: String) {
        val jobId = transactions.execute { _ ->
            val job = jobs.findByKey(key)!!
            assertThat(job.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(job.asiakirjat.map { it.asiakirjatyyppi }).containsExactly(type)
            job.id!!
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody("archived"))
        startAttempt()
        awaitDelivered(jobId)
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/unisign/elsa/archive/$code")
        assertThat(request.getHeader("X-Api-Key")).isEqualTo("integration-test")
        val entries = zipEntries(receivedZip(request))
        assertSavedPackage(jobId, entries)
        val requests = server.requestCount
        pollAndAwait()
        assertThat(taskCount(jobId)).isZero()
        assertThat(server.requestCount).isEqualTo(requests)
    }

    private fun startAttempt() {
        attemptPermits.release()
        scheduler.triggerCheckForDueExecutions()
    }

    private fun pollAndAwait() {
        val before = pollCount.get()
        scheduler.triggerCheckForDueExecutions()
        await().atMost(Duration.ofSeconds(30)).until { pollCount.get() > before }
    }

    private fun receivedZip(request: RecordedRequest): ByteArray {
        val body = request.body.readByteArray()
        val boundary = request.getHeader("Content-Type")!!.substringAfter("boundary=")
        val text = String(body, Charsets.ISO_8859_1)
        val start = text.indexOf("\r\n\r\n") + 4
        val end = text.lastIndexOf("\r\n--$boundary")
        assertThat(start).isGreaterThan(3)
        assertThat(end).isGreaterThan(start)
        return body.copyOfRange(start, end)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:helsinki-async-approval-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }

    companion object {
        private val server = MockWebServer().apply { start() }
        @JvmStatic @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            registry.add("application.arkistointi.hki.host") { server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/') }
        }
        @JvmStatic @AfterAll fun stopServer() = server.shutdown()
    }
}
