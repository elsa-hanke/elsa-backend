package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.toDescriptor
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Each method commits before the scheduler acknowledges or reschedules its execution. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class ArkistointiJobStateService(
    private val jobs: ArkistointiJobRepository,
    private val mapper: ObjectMapper,
    private val jdbc: JdbcTemplate,
    private val events: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun prepare(execution: ArkistointiJobExecution): PreparedArkistointiJob? {
        lockExecution(execution)
        val jobId = execution.jobId
        val job = jobs.findByIdForStateChange(jobId)
        if (job == null) {
            log.warn("Archive job missing [jobId={}]", jobId)
            return null
        }
        if (job.isFinished()) return null
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
        return PreparedArkistointiJob(
            jobId, job.university.required(), job.caseType.required(), payload,
            job.asiakirjat.sortedBy { it.jarjestysnumero }.map { it.toDescriptor() }
        )
    }

    /** Recheck the original execution after loading; release both locks before the network call. */
    fun checkBeforeSend(execution: ArkistointiJobExecution): Boolean {
        lockExecution(execution)
        val job = jobs.findByIdForStateChange(execution.jobId) ?: return false
        if (job.isFinished()) return false
        // A recovery handler may have committed ODOTTAA before rescheduling this same version.
        // Do not acknowledge that execution while its retry is still being arranged.
        if (job.status != ArkistointiJobStatus.KASITTELYSSA) throw StaleArkistointiExecutionException()
        return true
    }

    fun succeed(execution: ArkistointiJobExecution, externalId: String?) {
        val attempt = lockExecution(execution)
        val job = requireNotNull(jobs.findByIdForStateChange(execution.jobId))
        if (job.isFinished()) return
        ArkistointiExternalId.validate(externalId)
        job.startProcessing()
        job.transitionTo(ArkistointiJobStatus.LAHETETTY)
        job.externalId = externalId
        job.completedAt = Instant.now()
        job.errorCode = null
        job.lastError = null
        publishOutcome(job, attempt, ArkistointiAttemptCompleted.Outcome.SUCCESS)
    }

    /** Returns false once no further automatic execution is needed. */
    fun fail(execution: ArkistointiJobExecution, error: ArkistointiError, stopRetrying: Boolean): Boolean {
        val attempt = lockExecution(execution)
        val job = jobs.findByIdForStateChange(execution.jobId) ?: return false
        if (job.isFinished()) return false
        val retry = recordFailure(job, error, stopRetrying)
        publishOutcome(job, attempt, if (stopRetrying) ArkistointiAttemptCompleted.Outcome.TERMINAL_FAILURE
            else ArkistointiAttemptCompleted.Outcome.RETRYABLE_FAILURE, error.code)
        return retry
    }

    private fun lockExecution(execution: ArkistointiJobExecution): Int {
        // Lock order: scheduled_tasks -> arkistointi_job. Keep the scheduler's version stable until
        // the job update commits; a separate SELECT without FOR UPDATE would leave another race.
        val current = jdbc.query(
            "select consecutive_failures from scheduled_tasks where task_name = ? and task_instance = ? " +
                "and version = ? and picked = true for update",
            { row, _ -> row.getInt("consecutive_failures") + 1 }, execution.taskName, execution.taskInstanceId, execution.version
        ).singleOrNull()
        return current ?: throw StaleArkistointiExecutionException()
    }

    private fun publishOutcome(job: ArkistointiJob, attempt: Int, outcome: ArkistointiAttemptCompleted.Outcome, code: String? = null) {
        events.publishEvent(ArkistointiAttemptCompleted(job.id.required(), job.university.required(), job.caseType.required(), attempt, outcome, code))
    }

    private fun recordFailure(job: ArkistointiJob, error: ArkistointiError, stopRetrying: Boolean): Boolean {
        if (job.isFinished()) return false
        job.startProcessing()
        job.errorCode = error.code
        job.lastError = error.description
        job.transitionTo(
            if (stopRetrying) ArkistointiJobStatus.VAATII_TOIMENPITEITA else ArkistointiJobStatus.ODOTTAA
        )
        return !stopRetrying
    }

    private fun ArkistointiJob.startProcessing() {
        if (status == ArkistointiJobStatus.ODOTTAA) transitionTo(ArkistointiJobStatus.KASITTELYSSA)
    }

    /** Automatic processing is finished; a controlled manual retry may still be possible. */
    private fun ArkistointiJob.isFinished() = status == ArkistointiJobStatus.LAHETETTY || status == ArkistointiJobStatus.VAATII_TOIMENPITEITA
}
