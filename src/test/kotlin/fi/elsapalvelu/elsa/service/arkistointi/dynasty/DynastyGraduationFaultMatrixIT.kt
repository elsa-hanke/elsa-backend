package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiExecutionLock
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Stream
import javax.sql.DataSource
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

/**
 * Interrupts a Turku graduation delivery (case, task, three documents, archive) at every step in three ways and
 * checks that recovery converges: each Dynasty mutation is created exactly once, in order, and attachments always
 * reference the main document. Uses the real ledger, step executor, mapper, client and the operator resolve script
 * against a stateful fake Dynasty; the scheduler is replaced by explicit attempts and SQL state changes.
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DynastyGraduationFaultMatrixIT.Config::class])
class DynastyGraduationFaultMatrixIT {
    @Autowired private lateinit var ledger: DynastyStepLedger
    @Autowired private lateinit var jdbc: JdbcTemplate
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)
    private val mapper = jacksonObjectMapper()

    enum class Fault {
        /** Dynasty rejects the request (HTTP 400): nothing was created and the ledger intent is released. */
        REJECTED,
        /** Gateway error after Dynasty created the object: the operator confirms it with the real remote id. */
        UNCONFIRMED_CREATED,
        /** Gateway error although nothing was created: the operator discards the unfinished step. */
        UNCONFIRMED_NOT_CREATED
    }

    @Configuration
    @EnableTransactionManagement
    @Import(DynastyStepLedger::class, ArkistointiExecutionLock::class)
    class Config {
        @Bean fun dataSource(): DataSource = DriverManagerDataSource(database.jdbcUrl, database.username, database.password)
        @Bean fun transactionManager() = DataSourceTransactionManager(dataSource())
        @Bean fun jdbc(): JdbcTemplate = JdbcTemplate(dataSource()).also { DynastyStepJdbcFixture(it).createSchema() }
    }

    @ParameterizedTest(name = "{1} at {0}")
    @MethodSource("scenarios")
    fun `interrupted graduation delivery converges without repeating a Dynasty mutation`(step: String, fault: Fault) {
        val dynasty = FakeDynasty()
        val server = MockWebServer().apply { dispatcher = dynasty; start() }
        try {
            val properties = dynastyTestProperties(server.url("/").newBuilder().host("127.0.0.1").build().toString())
            val adapter = TurkuDynastyArkistointiAdapter(
                DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper),
                TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper)),
                DynastyStepExecutor(ledger, properties)
            )
            val job = stepFixture.execution(jobStatus = ArkistointiJobStatus.KASITTELYSSA, picked = true)
            (0..2).forEach { jdbc.update("insert into arkistointi_job_asiakirja values (?,?)", job.jobId, it) }
            val submission = dynastySubmission(mapper, jobId = job.jobId)
            dynasty.failNext(step, fault)

            val first = adapter.arkistoi(submission)

            assertThat(completedSteps(job)).describedAs("steps before the interruption").containsExactlyElementsOf(STEPS.takeWhile { it != step })
            if (fault == Fault.REJECTED) {
                assertThat(failureCode(first)).isEqualTo("DYNASTY_HTTP_REJECTED")
                assertThat(stepFixture.step(job.jobId, step)).describedAs("released intent").isNull()
            } else {
                assertThat(failureCode(first)).isEqualTo("DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(stepFixture.step(job.jobId, step)?.status).isEqualTo(DynastyStepStatus.ALOITETTU)
                val requestsBefore = dynasty.requests.size
                // Within the grace period the unfinished intent is retried later and nothing is sent.
                val pending = adapter.arkistoi(submission)
                assertThat(pending).isInstanceOf(ArkistointiResult.RetryableFailure::class.java)
                assertThat(failureCode(pending)).isEqualTo("DYNASTY_STEP_IN_PROGRESS")
                age(job, step)
                // After the grace period the step needs a human decision and still sends nothing.
                assertThat(failureCode(adapter.arkistoi(submission))).isEqualTo("DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(dynasty.requests.size).isEqualTo(requestsBefore)
                park(job)
                val confirm = fault == Fault.UNCONFIRMED_CREATED
                val result = resolve(job, step, if (confirm) dynasty.remoteIds.getValue(step) else "", if (confirm) "confirm" else "discard")
                assertThat(result.exitCode).describedAs(result.stderr).isZero()
                resume(job)
            }

            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
            assertThat(dynasty.created).describedAs("every mutation exactly once, in order").containsExactlyElementsOf(STEPS)
            assertThat(completedSteps(job)).containsExactlyElementsOf(STEPS)
            assertThat(dynasty.attachmentRequests).describedAs("attachments reference the main document").isNotEmpty().allMatch { "1078" in it }
            assertThat(dynasty.mainDocumentRequests).describedAs("main document references the task").isNotEmpty().allMatch { TASK_ID in it }
        } finally {
            server.shutdown()
        }
    }

    private fun failureCode(result: ArkistointiResult): String? = when (result) {
        is ArkistointiResult.RetryableFailure -> result.error.code
        is ArkistointiResult.PermanentFailure -> result.error.code
        is ArkistointiResult.Success -> null
    }

    private fun completedSteps(job: ArkistointiJobExecution): List<String> =
        stepFixture.steps(job.jobId).filter { it.status == DynastyStepStatus.VALMIS }.map { it.key }
            .sortedBy { STEPS.indexOf(it) }

    private fun age(job: ArkistointiJobExecution, step: String) {
        jdbc.update("update arkistointi_dynasty_step set started_at = started_at - interval '1 hour' where job_id=? and step_key=?", job.jobId, step)
    }

    /** The state the worker leaves behind when it parks a job for intervention. */
    private fun park(job: ArkistointiJobExecution) {
        jdbc.update("update arkistointi_job set status=? where id=?", ArkistointiJobStatus.VAATII_TOIMENPITEITA.name, job.jobId)
        jdbc.update("update scheduled_tasks set picked=false, execution_time=?::timestamptz where task_name=? and task_instance=?",
            "2500-01-01T12:00:00Z", job.taskName, job.taskInstanceId)
    }

    /** The state after the retry script and a new pick of the same execution. */
    private fun resume(job: ArkistointiJobExecution) {
        jdbc.update("update arkistointi_job set status=? where id=?", ArkistointiJobStatus.KASITTELYSSA.name, job.jobId)
        jdbc.update("update scheduled_tasks set picked=true where task_name=? and task_instance=?", job.taskName, job.taskInstanceId)
    }

    private fun resolve(job: ArkistointiJobExecution, step: String, remote: String, action: String) =
        database.execInContainer("psql", "-U", database.username, "-d", database.databaseName,
            "-v", "job_id=${job.jobId}", "-v", "step_key=$step", "-v", "remote_id=$remote", "-v", "action=$action", "-f", "/tmp/resolve.sql")

    /** Stateful Dynasty: creates objects only when it accepts a request and can fail one request once. */
    private class FakeDynasty : Dispatcher() {
        val requests = CopyOnWriteArrayList<String>()
        val created = CopyOnWriteArrayList<String>()
        val remoteIds = ConcurrentHashMap<String, String>()
        val attachmentRequests = CopyOnWriteArrayList<String>()
        val mainDocumentRequests = CopyOnWriteArrayList<String>()
        private var fault: Pair<String, Fault>? = null
        private var nextDocumentId = 1078L

        @Synchronized fun failNext(step: String, kind: Fault) { fault = step to kind }

        @Synchronized override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path!!
            if (path == "/token") return MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE)
            val body = request.body.clone().readUtf8()
            val step = when {
                path.startsWith("/api/v1/cases?") -> "CASE"
                path == "/api/v1/cases/415/tasks" -> "TASK"
                path == "/api/v1/documents" -> "DOCUMENT:" + DOCUMENT_FILE.find(body)!!.groupValues[1]
                path == "/api/v1/cases/415/state/Archived" -> "ARCHIVE_CASE"
                else -> return MockResponse().setResponseCode(404)
            }
            requests += step
            if (step == "DOCUMENT:0") mainDocumentRequests += body else if (step.startsWith("DOCUMENT:")) attachmentRequests += body
            val injected = fault?.takeIf { it.first == step }?.second
            if (injected != null) fault = null
            if (injected == Fault.REJECTED) return MockResponse().setResponseCode(400)
            if (injected == Fault.UNCONFIRMED_NOT_CREATED) return MockResponse().setResponseCode(503)
            val id = when {
                step == "CASE" -> "415"
                step == "TASK" -> TASK_ID
                step == "ARCHIVE_CASE" -> ""
                else -> (nextDocumentId++).toString()
            }
            created += step
            remoteIds[step] = id
            if (injected == Fault.UNCONFIRMED_CREATED) return MockResponse().setResponseCode(503)
            return MockResponse().setBody(if (step == "TASK") "\"$id\"" else id)
        }
    }

    companion object {
        private const val TASK_ID = "123e4567-e89b-12d3-a456-426614174000"
        private val DOCUMENT_FILE = Regex("filename=\"file(\\d+)\\.pdf\"")
        private val STEPS = listOf("CASE", "TASK", "DOCUMENT:0", "DOCUMENT:1", "DOCUMENT:2", "ARCHIVE_CASE")
        private val database = PostgreSQLContainer("postgres:16.9").apply {
            start()
            copyFileToContainer(
                MountableFile.forHostPath(Path.of("ops/arkistointi/dynasty-resolve-step.sql").toAbsolutePath().toString()),
                "/tmp/resolve.sql"
            )
        }

        @JvmStatic
        fun scenarios(): Stream<Arguments> = STEPS.flatMap { step -> Fault.entries.map { Arguments.of(step, it) } }.stream()
    }
}
