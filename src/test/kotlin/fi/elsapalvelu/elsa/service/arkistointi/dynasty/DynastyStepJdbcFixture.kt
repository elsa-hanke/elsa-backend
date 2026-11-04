package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

/** Small persistence fixture; tests retain the SQL mutations that define recovery/race scenarios. */
class DynastyStepJdbcFixture(private val jdbc: JdbcTemplate) {
    /** Minimal dependencies plus the real ledger migration, used only by isolated ledger/script databases. */
    fun createSchema() {
        jdbc.execute("create table scheduled_tasks(task_name text,task_instance text,version bigint,picked boolean," +
            "consecutive_failures integer default 0,execution_time timestamptz,primary key(task_name,task_instance))")
        jdbc.execute("create table arkistointi_job(id uuid primary key,status text)")
        jdbc.execute("create table arkistointi_job_asiakirja(job_id uuid,jarjestys integer)")
        val migration = javaClass.getResourceAsStream("/config/liquibase/changelog/20261101090000_added_arkistointi_dynasty_step.xml")!!
        migration.use {
            jdbc.execute(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).getElementsByTagName("sql").item(0).textContent)
        }
    }

    fun execution(
        jobStatus: ArkistointiJobStatus,
        picked: Boolean,
        executionTime: Instant? = null,
        taskName: String = "arkistointi-job",
        version: Long = 1
    ): ArkistointiJobExecution {
        val execution = ArkistointiJobExecution(taskName, UUID.randomUUID().toString(), version)
        jdbc.update("insert into arkistointi_job(id,status) values (?,?)", execution.jobId, jobStatus.name)
        jdbc.update("insert into scheduled_tasks(task_name,task_instance,version,picked,execution_time) values (?,?,?,?,?)",
            execution.taskName, execution.taskInstanceId, version, picked, executionTime?.let(Timestamp::from))
        return execution
    }

    /** String keys deliberately permit malformed operator input in resolve-script tests. */
    fun startedStep(execution: ArkistointiJobExecution, stepKey: String, age: Duration = Duration.ZERO) {
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,?,'ALOITETTU',?,clock_timestamp() - (?::bigint * interval '1 millisecond'))",
            execution.jobId, stepKey, execution.version, age.toMillis())
    }

    fun steps(jobId: UUID): List<Step> = jdbc.query(
        "select step_key,status,remote_id,execution_version,started_at,completed_at from arkistointi_dynasty_step where job_id=? order by step_key",
        { row, _ -> Step(
            row.getString("step_key"), DynastyStepStatus.valueOf(row.getString("status")), row.getString("remote_id"),
            row.getLong("execution_version"), row.getTimestamp("started_at").toInstant(), row.getTimestamp("completed_at")?.toInstant()
        ) }, jobId
    )

    fun step(jobId: UUID, stepKey: String): Step? = steps(jobId).singleOrNull { it.key == stepKey }

    data class Step(
        val key: String,
        val status: DynastyStepStatus,
        val remoteId: String?,
        val executionVersion: Long,
        val startedAt: Instant,
        val completedAt: Instant?
    )
}
