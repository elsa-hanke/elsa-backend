package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkeDelivery
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder

import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration
import com.github.kagkarlsson.scheduler.event.AbstractSchedulerListener
import com.github.kagkarlsson.scheduler.event.SchedulerListener.SchedulerEventType
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiDbSchedulerConfiguration
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobFailureHandler
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URI
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDate
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import javax.cache.Caching
import javax.sql.DataSource
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.sftp.server.FileHandle
import org.apache.sshd.sftp.server.SftpEventListener

/** HTTP approval, real PDF/snapshots/PostgreSQL, real scheduler/adapter and loopback SFTP. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///tampereAsyncApproval?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.tre.kaytossa=true",
    "application.arkistointi.hki.kaytossa=false",
    "application.arkistointi.tre.metadata.zip-metadata=true",
    "application.arkistointi.tre.metadata.organisation=TAU",
    "application.arkistointi.tre.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.valmistuminen.title=Valmistuminen",
    "application.arkistointi.tre.metadata.cases.valmistuminen.type=Valmistuminen",
    "application.arkistointi.tre.metadata.cases.valmistuminen.function=04.02.05",
    "application.arkistointi.tre.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.tre.metadata.cases.valmistuminen.documents.liite.retention-period=10",
    "application.arkistointi.tre.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.koejakso.title=Koejakson arviointi",
    "application.arkistointi.tre.metadata.cases.koejakso.type=Koejakson arviointi",
    "application.arkistointi.tre.metadata.cases.koejakso.function=04.01.04",
    "application.arkistointi.tre.metadata.cases.koejakso.documents.arviointi.retention-period=-1",
    "application.arkistointi.tre.metadata.cases.sopimus.async-enabled=false"
])
@Import(TampereAsyncApprovalIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TampereAsyncApprovalIT : AsyncArchiveItSupport() {
    @Autowired private lateinit var scheduler: Scheduler
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var properties: ApplicationProperties
    @Autowired private lateinit var configuration: ArkistointiConfigurationProvider
    @Autowired private lateinit var packageBuilder: AsyncSahkePackageBuilder
    @Autowired private lateinit var failureHandler: ArkistointiJobFailureHandler
    @Autowired private lateinit var dataSource: DataSource
    @MockitoBean lateinit var mail: MailService
    @TestBean(methodName = "createTransport") lateinit var tampereLouhiService: TampereLouhiService

    private val pollCount = AtomicInteger()

    @BeforeEach
    fun observeSchedulerPolls() {
        scheduler.registerSchedulerListener(object : AbstractSchedulerListener() {
            override fun onSchedulerEvent(type: SchedulerEventType) {
                if (type == SchedulerEventType.RAN_EXECUTE_DUE) pollCount.incrementAndGet()
            }
        })
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation approval sends exactly its saved documents to Louhi`(kind: String) {
        val fixture = graduationFixture(kind, YliopistoEnum.TAMPEREEN_YLIOPISTO, false)
        setSecurityContext(fixture.second, VASTUUHENKILO)
        testMockMvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${fixture.first}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null))).with(csrf()))
            .andExpect(status().isOk)
        assertDelivery("valmistuminen:${YliopistoEnum.TAMPEREEN_YLIOPISTO.name}:${fixture.first}",
            listOf(RecordType.YHTEENVETO, RecordType.LIITE), kind == "YEK")
    }

    @Test
    fun `koejakso approval sends its saved assessment to Louhi`() {
        val prepared = koejaksoFixture(YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.KOEJAKSO)
        val fixture = prepared.second to prepared.third
        setSecurityContext(fixture.second, VASTUUHENKILO)
        val dto = transactions.execute { _ ->
            assessmentMapper.toDto(assessments.findById(fixture.first).orElseThrow()).apply { koejaksoHyvaksytty = true }
        }
        testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
            .andExpect(status().isOk)
        assertDelivery("koejakso-arvio:${YliopistoEnum.TAMPEREEN_YLIOPISTO.name}:${fixture.first}", listOf(RecordType.ARVIOINTI), false)
    }

    @Test
    fun `context shutdown drains the active scheduler send before destroying Louhi client`() {
        LoopbackLouhiServer().use { receiver ->
            val writing = CountDownLatch(1)
            val release = CountDownLatch(1)
            val destroyed = AtomicBoolean(false)
            val outcome = AtomicReference<ArkistointiResult>()
            val adapter = AtomicReference<LouhiAsyncArkistointiAdapter>()
            receiver.subsystem.addSftpEventListener(object : SftpEventListener {
                override fun writing(session: ServerSession, handle: String, file: FileHandle,
                    offset: Long, data: ByteArray, dataOffset: Int, dataLen: Int) {
                    writing.countDown()
                    check(release.await(15, TimeUnit.SECONDS))
                }
            })
            val submission = shutdownSubmission()
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DbSchedulerAutoConfiguration::class.java))
                .withUserConfiguration(ArkistointiDbSchedulerConfiguration::class.java,
                    ArkistointiAdapterRegistry::class.java, AsyncSahkeDelivery::class.java, LouhiAsyncArkistointiAdapter::class.java)
                .withBean(ApplicationProperties::class.java, Supplier { properties })
                .withBean(ArkistointiConfigurationProvider::class.java, Supplier { configuration })
                .withBean(AsyncSahkePackageBuilder::class.java, Supplier { packageBuilder })
                .withBean(ArkistointiJobFailureHandler::class.java, Supplier { failureHandler })
                .withBean(DataSource::class.java, Supplier { DelegatingDataSource(dataSource) })
                .withBean(TampereLouhiService::class.java, Supplier { receiver.client { destroyed.set(true) } },
                    { definition -> definition.destroyMethodName = "close" })
                .withBean(ArkistointiJobProcessor::class.java, Supplier {
                    ArkistointiJobProcessor {
                        val result = adapter.get().arkistoi(submission)
                        outcome.set(result)
                        check(result is ArkistointiResult.Success)
                    }
                })
                .withPropertyValues("db-scheduler.enabled=true", "db-scheduler.polling-interval=1h",
                    "db-scheduler.threads=1", "db-scheduler.shutdown-max-wait=20s")
                .run { context ->
                    assertThat(context).hasNotFailed()
                    adapter.set(context.getBean(LouhiAsyncArkistointiAdapter::class.java))
                    val worker = context.getBean(Scheduler::class.java)
                    worker.schedule(ARKISTOINTI_JOB_TASK.instance(submission.jobId.toString()).scheduledTo(Instant.now()))
                    worker.triggerCheckForDueExecutions()
                    assertThat(writing.await(10, TimeUnit.SECONDS)).isTrue()
                    val executor = Executors.newSingleThreadExecutor()
                    try {
                        val close = executor.submit { context.sourceApplicationContext.close() }
                        await().atMost(Duration.ofSeconds(5)).until { worker.schedulerState.isShuttingDown }
                        assertThat(destroyed).isFalse()
                        assertThat(close.isDone).isFalse()
                        release.countDown()
                        close.get(20, TimeUnit.SECONDS)
                        assertThat(outcome.get()).isInstanceOf(ArkistointiResult.Success::class.java)
                        assertThat(destroyed).isTrue()
                        assertThat(taskCount(submission.jobId)).isZero()
                        Files.list(receiver.remote.resolve("Finished/ELSA")).use { assertThat(it.count()).isEqualTo(1) }
                    } finally {
                        release.countDown()
                        executor.shutdownNow()
                    }
                }
        }
    }

    private fun shutdownSubmission(): ArkistointiSubmission {
        val bytes = PdfTestData.certificate()
        val payload = objectMapper.writeValueAsString(mapOf(
            "university" to YliopistoEnum.TAMPEREEN_YLIOPISTO.name, "yek" to false,
            "caseId" to 1, "opintooikeusId" to 1, "approvalDate" to LocalDate.now().toString(),
            "firstName" to "Lifecycle", "lastName" to "Test",
            "documents" to listOf(ArkistointiDocumentIdentity(10, 20, RecordType.ARVIOINTI, 0))
        ))
        return ArkistointiSubmission(UUID.randomUUID(), YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.KOEJAKSO, payload,
            listOf(LoadedArkistointiAsiakirja(20, RecordType.ARVIOINTI, 0, "assessment.pdf", "application/pdf",
                ArkistointiChecksum.sha256(bytes), bytes)))
    }

    private fun assertDelivery(key: String, types: List<RecordType>, yek: Boolean) {
        val jobId = transactions.execute { _ ->
            val job = jobs.findByKey(key)!!
            assertThat(job.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(job.asiakirjat.sortedBy { it.jarjestysnumero }.map { it.asiakirjatyyppi }).containsExactlyElementsOf(types)
            job.id!!
        }
        scheduler.triggerCheckForDueExecutions()
        awaitDelivered(jobId)
        val finished = server.remote.resolve(if (yek) "Finished/YEK" else "Finished/ELSA")
        val zipPath = Files.list(finished).use { paths -> paths.filter { it.toString().endsWith(".zip") }.toList().single() }
        assertSavedPackage(jobId, zipEntries(Files.readAllBytes(zipPath)))
        // An extra poll after completion must not send the job again.
        val published = Files.getLastModifiedTime(zipPath)
        pollAndAwait()
        assertThat(taskCount(jobId)).isZero()
        assertThat(Files.getLastModifiedTime(zipPath)).isEqualTo(published)
        Files.delete(zipPath)
    }

    private fun pollAndAwait() {
        val before = pollCount.get()
        scheduler.triggerCheckForDueExecutions()
        await().atMost(Duration.ofSeconds(30)).until { pollCount.get() > before }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:tampere-async-approval-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }

    companion object {
        private val server by lazy { LoopbackLouhiServer() }
        @JvmStatic fun createTransport(): TampereLouhiService = server.client()
        @JvmStatic @AfterAll fun stopServer() = server.close()
    }
}
