package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiExecutionLock
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.StaleArkistointiExecutionException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/** Commit intent before the request; a crash never silently authorizes another create. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class DynastyStepLedger(
    private val jdbc: JdbcTemplate,
    private val executionLock: ArkistointiExecutionLock
) {
    sealed interface Decision {
        data object Proceed : Decision
        data class Done(val remoteId: String?) : Decision
        data object InProgress : Decision
        data object Unconfirmed : Decision
    }

    fun begin(execution: ArkistointiJobExecution, key: DynastyStepKey<*>, inProgressGrace: Duration): Decision {
        executionLock.lock(execution)
        val jobStatus = jdbc.query("select status from arkistointi_job where id=? for update", jobStatusMapper, execution.jobId).singleOrNull()
        if (jobStatus != ArkistointiJobStatus.KASITTELYSSA) throw StaleArkistointiExecutionException()
        // Use the database wall clock after acquiring locks, not JVM time or transaction start time.
        val rows = jdbc.query(
            "select status, remote_id, started_at + (?::bigint * interval '1 millisecond') > clock_timestamp() as in_progress " +
                "from arkistointi_dynasty_step where job_id=? and step_key=?",
            stepRowMapper, inProgressGrace.toMillis(), execution.jobId, key.databaseKey
        )
        if (rows.isNotEmpty()) {
            val row = rows.single()
            if (row.status == DynastyStepStatus.VALMIS) return Decision.Done(row.remoteId)
            return if (row.inProgress) Decision.InProgress else Decision.Unconfirmed
        }
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) values (?,?,?,?,clock_timestamp())",
            execution.jobId, key.databaseKey, DynastyStepStatus.ALOITETTU.name, execution.version)
        return Decision.Proceed
    }

    /** A late response still records a fact, but only for the attempt which started this step. */
    fun complete(jobId: UUID, key: DynastyStepKey<*>, version: Long, remoteId: String?): Boolean = jdbc.update(
        "update arkistointi_dynasty_step set status=?,remote_id=?,completed_at=clock_timestamp() where job_id=? and " +
            "step_key=? and execution_version=? and status=?",
        DynastyStepStatus.VALMIS.name, remoteId, jobId, key.databaseKey, version, DynastyStepStatus.ALOITETTU.name
    ) == 1

    fun abandon(jobId: UUID, key: DynastyStepKey<*>, version: Long) {
        jdbc.update("delete from arkistointi_dynasty_step where job_id=? and step_key=? and execution_version=? and status=?",
            jobId, key.databaseKey, version, DynastyStepStatus.ALOITETTU.name)
    }
    private data class StepRow(val status: DynastyStepStatus, val remoteId: String?, val inProgress: Boolean)

    private val jobStatusMapper = RowMapper { row, _ -> ArkistointiJobStatus.valueOf(row.getString("status")) }
    private val stepRowMapper = RowMapper { row, _ ->
        StepRow(DynastyStepStatus.valueOf(row.getString("status")), row.getString("remote_id"), row.getBoolean("in_progress"))
    }
}

enum class DynastyStepStatus { ALOITETTU, VALMIS }
