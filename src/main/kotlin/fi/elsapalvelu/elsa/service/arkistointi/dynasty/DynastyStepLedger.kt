package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiExecutionLock
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.StaleArkistointiExecutionException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/** Commit intent before the request; a crash never silently authorizes another create. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class DynastyStepLedger(
    private val jdbc: JdbcTemplate,
    private val properties: ApplicationProperties,
    private val executionLock: ArkistointiExecutionLock
) {
    sealed interface Decision {
        data object Proceed : Decision
        data class Done(val remoteId: String?) : Decision
        data object InProgress : Decision
        data object Unconfirmed : Decision
    }

    fun begin(execution: ArkistointiJobExecution, key: String): Decision {
        executionLock.lock(execution)
        val job = jdbc.queryForList("select status from arkistointi_job where id=? for update", execution.jobId).singleOrNull()
        if (job?.get("status") != "KASITTELYSSA") throw StaleArkistointiExecutionException()
        val rows = jdbc.queryForList("select status, remote_id, started_at from arkistointi_dynasty_step where job_id=? and step_key=?", execution.jobId, key)
        if (rows.isNotEmpty()) {
            val row = rows.single()
            if (row["status"] == "VALMIS") return Decision.Done(row["remote_id"] as String?)
            val started = (row["started_at"] as java.sql.Timestamp).toInstant()
            return if (started.plus(properties.getArkistointi().getTurku().dynasty.inProgressGrace).isAfter(Instant.now())) Decision.InProgress else Decision.Unconfirmed
        }
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) values (?,?,'ALOITETTU',?,CURRENT_TIMESTAMP)",
            execution.jobId, key, execution.version)
        return Decision.Proceed
    }

    /** A late response still records a fact, but only for the attempt which started this step. */
    fun complete(jobId: UUID, key: String, version: Long, remoteId: String?): Boolean = jdbc.update(
        "update arkistointi_dynasty_step set status='VALMIS',remote_id=?,completed_at=CURRENT_TIMESTAMP where job_id=? and " +
            "step_key=? and execution_version=? and status='ALOITETTU'",
        remoteId, jobId, key, version
    ) == 1

    fun abandon(jobId: UUID, key: String, version: Long) {
        jdbc.update("delete from arkistointi_dynasty_step where job_id=? and step_key=? and execution_version=? and status='ALOITETTU'", jobId, key, version)
    }
}
