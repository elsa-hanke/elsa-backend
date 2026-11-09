package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiExecutionLock
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import java.util.UUID
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Interrupts a Turku graduation delivery (case, task, three documents, archive) at every step in three ways and
 * checks that recovery converges: each Dynasty mutation is created exactly once, in order, and attachments always
 * reference the main document. Uses the real ledger, step executor, mapper, client and both operator scripts
 * (resolve and retry) against a stateful fake Dynasty. The scheduler is replaced by explicit attempts, but every
 * attempt runs under a new execution version like in production, so steps completed by an earlier version must be
 * reused. A permanent failure parks the job and a retryable one backs it off, exactly as the worker decides.
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
        @Bean fun dataSource(): DataSource = ops.dataSource
        @Bean fun transactionManager() = DataSourceTransactionManager(dataSource())
        @Bean fun jdbc(): JdbcTemplate = JdbcTemplate(dataSource())
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
            var execution = stepFixture.execution(jobStatus = ArkistointiJobStatus.KASITTELYSSA, picked = true)
            (0..2).forEach { jdbc.update("insert into arkistointi_job_asiakirja values (?,?)", execution.jobId, it) }
            val jobId = execution.jobId
            fun attempt() = adapter.arkistoi(dynastySubmission(mapper, jobId = jobId, version = execution.version))
            dynasty.failNext(step, fault)

            val first = attempt()

            // Both a rejection and an unconfirmed send are permanent failures, which park the job.
            assertThat(first).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            assertThat(completedSteps(jobId)).describedAs("steps before the interruption")
                .containsExactlyElementsOf(STEPS.takeWhile { it != step })
            park(execution)
            if (fault == Fault.REJECTED) {
                assertThat(failureCode(first)).isEqualTo("DYNASTY_HTTP_REJECTED")
                assertThat(stepFixture.step(jobId, step)).describedAs("released intent").isNull()
                execution = retry(execution)
            } else {
                assertThat(failureCode(first)).isEqualTo("DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(stepFixture.step(jobId, step)?.status).isEqualTo(DynastyStepStatus.ALOITETTU)
                val requestsBefore = dynasty.requests.size
                // An immediate manual retry finds the unfinished intent inside the grace period: back off, send nothing.
                execution = retry(execution)
                val pending = attempt()
                assertThat(pending).isInstanceOf(ArkistointiResult.RetryableFailure::class.java)
                assertThat(failureCode(pending)).isEqualTo("DYNASTY_STEP_IN_PROGRESS")
                backOff(execution)
                // Once the grace period is over the step needs a human decision and still sends nothing.
                age(jobId, step)
                execution = claim(execution)
                val again = attempt()
                assertThat(again).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
                assertThat(failureCode(again)).isEqualTo("DYNASTY_DELIVERY_UNCONFIRMED")
                assertThat(dynasty.requests.size).isEqualTo(requestsBefore)
                park(execution)
                val confirm = fault == Fault.UNCONFIRMED_CREATED
                val resolved = ops.resolveStep(
                    jobId, step, if (confirm) dynasty.remoteIds.getValue(step) else "", if (confirm) "confirm" else "discard"
                )
                assertThat(resolved.exitCode).describedAs(resolved.stderr).isZero()
                execution = retry(execution)
            }

            assertThat(execution.version).describedAs("steps completed by version 1 are reused by a later execution").isGreaterThan(1)
            assertThat(attempt()).isEqualTo(ArkistointiResult.Success("415"))
            assertThat(dynasty.created).describedAs("every mutation exactly once, in order").containsExactlyElementsOf(STEPS)
            assertThat(completedSteps(jobId)).containsExactlyElementsOf(STEPS)
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

    private fun completedSteps(jobId: UUID): List<String> =
        stepFixture.steps(jobId).filter { it.status == DynastyStepStatus.VALMIS }.map { it.key }
            .sortedBy { STEPS.indexOf(it) }

    private fun age(jobId: UUID, step: String) {
        jdbc.update("update arkistointi_dynasty_step set started_at = started_at - interval '1 hour' where job_id=? and step_key=?", jobId, step)
    }

    /** What the worker leaves behind for a permanent failure. */
    private fun park(job: ArkistointiJobExecution) {
        jdbc.update("update arkistointi_job set status=? where id=?", ArkistointiJobStatus.VAATII_TOIMENPITEITA.name, job.jobId)
        jdbc.update("update scheduled_tasks set picked=false, execution_time=?::timestamptz where task_name=? and task_instance=?",
            "2500-01-01T12:00:00Z", job.taskName, job.taskInstanceId)
    }

    /** What the worker leaves behind for a retryable failure. */
    private fun backOff(job: ArkistointiJobExecution) {
        jdbc.update("update arkistointi_job set status=? where id=?", ArkistointiJobStatus.ODOTTAA.name, job.jobId)
        jdbc.update("update scheduled_tasks set picked=false, execution_time=clock_timestamp() + interval '1 minute' " +
            "where task_name=? and task_instance=?", job.taskName, job.taskInstanceId)
    }

    /** The operator retry script, then the scheduler picking the task again. */
    private fun retry(job: ArkistointiJobExecution): ArkistointiJobExecution {
        val result = ops.retryJob(job.jobId)
        assertThat(result.exitCode).describedAs(result.stderr).isZero()
        return claim(job)
    }

    /** The scheduler picks the task (version + 1) and the worker moves the job to processing. */
    private fun claim(job: ArkistointiJobExecution): ArkistointiJobExecution {
        jdbc.update("update scheduled_tasks set picked=true, version=version+1 where task_name=? and task_instance=?",
            job.taskName, job.taskInstanceId)
        jdbc.update("update arkistointi_job set status=? where id=?", ArkistointiJobStatus.KASITTELYSSA.name, job.jobId)
        val version = jdbc.queryForObject("select version from scheduled_tasks where task_name=? and task_instance=?",
            Long::class.javaObjectType, job.taskName, job.taskInstanceId)!!
        return ArkistointiJobExecution(job.taskName, job.taskInstanceId, version)
    }

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
        const val TASK_ID = "123e4567-e89b-12d3-a456-426614174000"
        val DOCUMENT_FILE = Regex("filename=\"file(\\d+)\\.pdf\"")
        val STEPS = listOf("CASE", "TASK", "DOCUMENT:0", "DOCUMENT:1", "DOCUMENT:2", "ARCHIVE_CASE")
        val ops = DynastyOpsDatabase().start()

        @JvmStatic
        fun scenarios(): Stream<Arguments> = STEPS.flatMap { step -> Fault.entries.map { Arguments.of(step, it) } }.stream()
    }
}
