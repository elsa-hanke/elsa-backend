package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

const val MDC_ARCHIVE_JOB_ID_KEY = "archiveJobId"

/**
 * Puts the archive job id and the user who requested the job into MDC while a scheduler thread works on it.
 *
 * db-scheduler threads never inherit the HTTP request's MDC, so asynchronous archive logs could not be found
 * by userId. The requesting user is read from the job row (written at enqueue time) and uses the same
 * [MDC_USER_ID_KEY] as request logs, so one userId search covers both. This is logging only: a failed lookup
 * must never change how an execution is processed, acknowledged or retried, so it falls back to the job id.
 */
@Component
class ArkistointiJobLogContext(private val jobs: ArkistointiJobRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> withJob(taskInstanceId: String, block: () -> T): T {
        val jobId = parseJobId(taskInstanceId) ?: return block()
        return withMdc(MDC_ARCHIVE_JOB_ID_KEY, jobId.toString()) {
            val userId = requestedByUserId(jobId)
            if (userId == null) block() else withMdc(MDC_USER_ID_KEY, userId, block)
        }
    }

    private fun parseJobId(taskInstanceId: String): UUID? = try {
        UUID.fromString(taskInstanceId)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun requestedByUserId(jobId: UUID): String? = try {
        jobs.findRequestedByUserId(jobId)
    } catch (_: Exception) {
        // No exception text: it may carry SQL details, and worker logs must stay free of them.
        log.debug("Archive job user lookup failed [jobId={}]", jobId)
        null
    }

    private fun <T> withMdc(key: String, value: String, block: () -> T): T {
        val previous = MDC.get(key)
        MDC.put(key, value)
        try {
            return block()
        } finally {
            if (previous == null) MDC.remove(key) else MDC.put(key, previous)
        }
    }
}
