package fi.elsapalvelu.elsa.service.arkistointi.job

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.required
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Each method commits before the scheduler acknowledges or reschedules its execution. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class ArkistointiJobStateService(
    private val jobs: ArkistointiJobRepository,
    private val loader: ArkistointiAsiakirjaLoader,
    private val mapper: ObjectMapper,
    private val jdbc: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun prepare(execution: ArkistointiJobExecution): ArkistointiSubmission? {
        lockExecution(execution)
        val jobId = execution.jobId
        val job = jobs.findByIdForStateChange(jobId)
        if (job == null) {
            log.warn("Archive job missing [jobId={}]", jobId)
            return null
        }
        if (job.isTerminal()) return null
        job.startProcessing()
        val payload = job.payload.required()
        val validPayload = try {
            mapper.readTree(payload)?.isObject == true
        } catch (_: com.fasterxml.jackson.core.JsonProcessingException) {
            false
        }
        if (!validPayload || job.asiakirjat.isEmpty()) {
            throw ArkistointiProcessingException(
                ArkistointiError.create("ARCHIVE_JOB_INVALID", "Arkistointijobin aineisto on virheellinen"),
                retryable = false
            )
        }
        return ArkistointiSubmission(
            jobId, job.university.required(), job.caseType.required(), payload,
            job.asiakirjat.sortedBy { it.jarjestysnumero }.map(loader::load)
        )
    }

    fun succeed(execution: ArkistointiJobExecution, externalId: String?) {
        lockExecution(execution)
        val job = requireNotNull(jobs.findByIdForStateChange(execution.jobId))
        if (job.isTerminal()) return
        ArkistointiExternalId.validate(externalId)
        job.startProcessing()
        job.transitionTo(ArkistointiJobStatus.LAHETETTY)
        job.externalId = externalId
        job.completedAt = Instant.now()
        job.errorCode = null
        job.lastError = null
    }

    /** Returns false once no further automatic execution is needed. */
    fun fail(execution: ArkistointiJobExecution, error: ArkistointiError, terminal: Boolean): Boolean {
        lockExecution(execution)
        val job = jobs.findByIdForStateChange(execution.jobId) ?: return false
        return recordFailure(job, error, terminal)
    }

    private fun lockExecution(execution: ArkistointiJobExecution) {
        // Lock order: scheduled_tasks -> arkistointi_job. Keep the scheduler's version stable until
        // the job update commits; a separate SELECT without FOR UPDATE would leave another race.
        val current = jdbc.query(
            "select version from scheduled_tasks where task_name = ? and task_instance = ? " +
                "and version = ? and picked = true for update",
            { _, _ -> true }, execution.taskName, execution.taskInstanceId, execution.version
        ).isNotEmpty()
        if (!current) throw StaleArkistointiExecutionException()
    }

    private fun recordFailure(job: ArkistointiJob, error: ArkistointiError, terminal: Boolean): Boolean {
        if (job.isTerminal()) return false
        job.startProcessing()
        job.errorCode = error.code
        job.lastError = error.description
        job.transitionTo(
            if (terminal) ArkistointiJobStatus.VAATII_TOIMENPITEITA else ArkistointiJobStatus.ODOTTAA
        )
        return !terminal
    }

    private fun ArkistointiJob.startProcessing() {
        // KASITTELYSSA is valid after db-scheduler recovers an interrupted execution.
        if (status == ArkistointiJobStatus.ODOTTAA) transitionTo(ArkistointiJobStatus.KASITTELYSSA)
    }

    private fun ArkistointiJob.isTerminal() =
        status == ArkistointiJobStatus.LAHETETTY || status == ArkistointiJobStatus.VAATII_TOIMENPITEITA
}
