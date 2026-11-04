package fi.elsapalvelu.elsa.service.arkistointi.siilo

import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveSchedulerTestControl
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import java.time.Duration
import java.time.Instant
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status



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





    @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
    @MockitoBean lateinit var mail: MailService

    private lateinit var schedulerControl: ArchiveSchedulerTestControl

    @BeforeEach
    fun resetReceiver() {
        schedulerControl = ArchiveSchedulerTestControl(scheduler, processor)
        pollAndAwait()

        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Remove requests left by a failed previous assertion.
        }
    }

    @AfterEach
    fun clearPendingExecutions() {
        server.enqueue(MockResponse().setResponseCode(400))
        schedulerControl.releasePendingAttempts()
        await().atMost(Duration.ofSeconds(30)).until { scheduler.currentlyExecuting.isEmpty() }
        jdbc.queryForList("select task_instance from scheduled_tasks where task_name = 'arkistointi-job'", String::class.java)
            .forEach { scheduler.cancel(TaskInstanceId.of("arkistointi-job", it)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation approval delivers only Helsinki configured documents`(kind: String) {
        val fixture = graduationFixture(kind, YliopistoEnum.HELSINGIN_YLIOPISTO, true)
        setSecurityContext(fixture.second, VASTUUHENKILO)
        approveGraduation(fixture.first)
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

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `receiver rejection or lost response parks delivery without rolling back approval`(lostResponse: Boolean) {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val job = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!
        server.enqueue(if (lostResponse) MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST)
            else MockResponse().setResponseCode(400).setBody("private response"))
        startAttempt()
        awaitParked(job.id!!)
        assertThat(jobs.findById(job.id!!).orElseThrow().errorCode)
            .isEqualTo(if (lostResponse) "SIILO_DELIVERY_UNCONFIRMED" else "SIILO_HTTP_REJECTED")
        assertThat(assessments.findById(id).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
        server.takeRequest(5, TimeUnit.SECONDS)!!
        val requests = server.requestCount
        val attempts = org.mockito.Mockito.mockingDetails(processor).invocations.size
        pollAndAwait()
        assertThat(server.requestCount).isEqualTo(requests)
        assertThat(org.mockito.Mockito.mockingDetails(processor).invocations.size).isEqualTo(attempts)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
    }

    private fun approveKoejakso(type: CaseType): Long {
        val fixture = koejaksoFixture(YliopistoEnum.HELSINGIN_YLIOPISTO, type)
        val result = if (type == CaseType.SOPIMUS) approveContract(fixture) else approveAssessment(fixture)
        result.andExpect(status().isOk)
        return if (type == CaseType.SOPIMUS) fixture.first else fixture.second
    }

    @Test
    fun `accepted Siilo delivery is parked when success commit fails`() {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val jobId = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!.id!!
        val before = server.requestCount
        withSuccessCommitFailure {
            server.enqueue(MockResponse().setResponseCode(200))
            startAttempt()
            awaitParked(jobId)
        }
        assertThat(jobs.findById(jobId).orElseThrow().errorCode).isEqualTo("ARCHIVE_SUCCESS_PERSISTENCE_FAILED")
        pollAndAwait()
        assertThat(server.requestCount).isEqualTo(before + 1)
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

    private fun startAttempt() = schedulerControl.startAttempt()

    private fun pollAndAwait() = schedulerControl.pollAndAwait()

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
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:helsinki-async-approval-it", javaClass.classLoader, customizer)
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
