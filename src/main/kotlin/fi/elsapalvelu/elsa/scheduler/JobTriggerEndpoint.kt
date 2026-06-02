package fi.elsapalvelu.elsa.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

/**
 * Actuator endpoint for manually triggering scheduled jobs from the bastion host.
 *
 * Available at: POST /management/jobtrigger/{jobName}  (port 8081, VPC-internal only)
 *
 * Jobs are auto-discovered via Spring injection of all [TriggerableJob] beans.
 * To make a new scheduler manually triggerable, simply implement [TriggerableJob]
 * (or extend [AbstractTriggerableJob]) — no changes needed here.
 */
@Component
@Endpoint(id = "jobtrigger")
class JobTriggerEndpoint(
    jobs: List<TriggerableJob>,
    private val jdbcTemplate: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val jobRegistry: Map<String, TriggerableJob> = jobs.associateBy { it.jobName }

    @WriteOperation
    fun trigger(@Selector jobName: String): WebEndpointResponse<Map<String, String>> {
        val job = jobRegistry[jobName]
            ?: return WebEndpointResponse(
                mapOf(
                    "status" to "error",
                    "message" to "Unknown job: '$jobName'. Valid jobs: ${jobRegistry.keys.sorted().joinToString()}"
                ),
                400
            )

        val isLocked = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM shedlock WHERE name = ? AND lock_until > ?",
            Int::class.java,
            jobName,
            Timestamp.from(Instant.now())
        )?.let { it > 0 } ?: false

        if (isLocked) {
            return WebEndpointResponse(
                mapOf(
                    "status" to "conflict",
                    "message" to "Job '$jobName' is currently running or locked. Try again later."
                ),
                409
            )
        }

        return try {
            log.info("Manually triggering job: $jobName")
            job.runJob()
            log.info("Manual job trigger completed: $jobName")
            WebEndpointResponse(
                mapOf("status" to "ok", "message" to "Job '$jobName' triggered successfully."),
                200
            )
        } catch (e: Exception) {
            log.error("Manual job trigger failed for '$jobName': ${e.message}", e)
            WebEndpointResponse(
                mapOf("status" to "error", "message" to "Job '$jobName' failed: ${e.message}"),
                500
            )
        }
    }
}

