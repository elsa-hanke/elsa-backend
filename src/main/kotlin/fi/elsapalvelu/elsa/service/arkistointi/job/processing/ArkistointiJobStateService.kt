package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.toDescriptor
import java.time.Instant
import org.slf4j.LoggerFactory
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
    private val executionLock: ArkistointiExecutionLock,
    private val events: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun prepare(execution: ArkistointiJobExecution): PreparedArkistointiJob? {
        executionLock.lock(execution)
        val jobId = execution.jobId
        val job = jobs.findByIdForStateChange(jobId)
        if (job == null) {
            log.warn("Archive job missing [jobId={}]", jobId)
            return null
        }
        if (job.isVaatiiToimenpiteita()) {
            throw ParkArkistointiExecutionException()
        }
        if (job.isFinished()) {
            return null
        }
        job.startProcessingIfWaiting()
        val payload = job.payload.required()
        val validPayload = try {
            mapper.readTree(payload)?.isObject == true
        } catch (_: JsonProcessingException) {
            false
        }
        if (!validPayload || job.asiakirjat.isEmpty()) {
            val error = ArkistointiError.create("ARCHIVE_JOB_INVALID", "Arkistointijobin aineisto on virheellinen")
            throw ArkistointiProcessingException(error, retryable = false)
        }
        return PreparedArkistointiJob(jobId, job.university.required(), job.caseType.required(), payload,
            job.asiakirjat.sortedBy { it.jarjestysnumero }.map { it.toDescriptor() }
        )
    }

    /** Recheck the original execution after loading; release both locks before the network call. */
    fun checkBeforeSend(execution: ArkistointiJobExecution): Boolean {
        executionLock.lock(execution)
        val job = jobs.findByIdForStateChange(execution.jobId) ?: return false
        if (job.isVaatiiToimenpiteita()) {
            throw ParkArkistointiExecutionException()
        }
        if (job.isFinished()) {
            return false
        }
        // A recovery handler may have committed ODOTTAA before rescheduling this same version.
        // Do not acknowledge that execution while its retry is still being arranged.
        if (job.status != ArkistointiJobStatus.KASITTELYSSA) {
            throw StaleArkistointiExecutionException()
        }
        return true
    }

    fun succeed(execution: ArkistointiJobExecution, externalId: String?) {
        val attempt = executionLock.lock(execution)
        val job = requireNotNull(jobs.findByIdForStateChange(execution.jobId))
        if (job.isVaatiiToimenpiteita()) {
            throw ParkArkistointiExecutionException()
        }
        if (job.isFinished()) {
            return
        }
        ArkistointiExternalId.validate(externalId)
        job.startProcessingIfWaiting()
        job.transitionTo(ArkistointiJobStatus.LAHETETTY)
        job.externalId = externalId
        job.completedAt = Instant.now()
        job.errorCode = null
        job.lastError = null
        publishAttemptCompleted(job, attempt, ArkistointiAttemptCompleted.Result.SUCCESS)
    }

    fun fail(execution: ArkistointiJobExecution, error: ArkistointiError, stopRetrying: Boolean): Boolean =
        failExecution(execution, error, stopRetrying, deadExecution = false)

    fun recoverDeadExecution(execution: ArkistointiJobExecution, error: ArkistointiError, stopRetrying: Boolean): Boolean =
        failExecution(execution, error, stopRetrying, deadExecution = true)

    private fun failExecution(
        execution: ArkistointiJobExecution, error: ArkistointiError, stopRetrying: Boolean, deadExecution: Boolean
    ): Boolean {
        val attempt = executionLock.lock(execution)
        val job = jobs.findByIdForStateChange(execution.jobId) ?: return false
        if (job.isFinished()) {
            return false
        }
        val unknownDelivery = deadExecution && job.status == ArkistointiJobStatus.KASITTELYSSA
        val recordedError = if (unknownDelivery) ArkistointiError.create(
            "ARCHIVE_EXECUTION_LOST", "Arkistoinnin suorituksen yhteys katkesi; toimitus on tarkistettava ennen uusintaa"
        ) else error
        val terminal = stopRetrying || unknownDelivery
        val retry = recordFailure(job, recordedError, terminal)
        val result = if (terminal) ArkistointiAttemptCompleted.Result.TERMINAL_FAILURE else ArkistointiAttemptCompleted.Result.RETRYABLE_FAILURE
        publishAttemptCompleted(job, attempt, result, recordedError.code)
        return retry
    }

    fun requiresIntervention(execution: ArkistointiJobExecution): Boolean {
        executionLock.lock(execution)
        return jobs.findByIdForStateChange(execution.jobId)?.status == ArkistointiJobStatus.VAATII_TOIMENPITEITA
    }

    private fun publishAttemptCompleted(job: ArkistointiJob, attempt: Int, outcome: ArkistointiAttemptCompleted.Result, code: String? = null) {
        events.publishEvent(ArkistointiAttemptCompleted(job.id.required(), job.university.required(), job.caseType.required(), attempt, outcome, code))
    }

    private fun recordFailure(job: ArkistointiJob, error: ArkistointiError, stopRetrying: Boolean): Boolean {
        if (job.isFinished()) {
            return false
        }
        job.startProcessingIfWaiting()
        job.errorCode = error.code
        job.lastError = error.description
        val newStatus = if (stopRetrying) ArkistointiJobStatus.VAATII_TOIMENPITEITA else ArkistointiJobStatus.ODOTTAA
        job.transitionTo(newStatus)
        return !stopRetrying
    }
}
