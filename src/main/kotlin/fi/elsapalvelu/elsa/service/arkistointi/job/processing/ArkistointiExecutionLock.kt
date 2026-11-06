package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Lock scheduled_tasks before arkistointi_job; keep ownership stable until the caller commits. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
class ArkistointiExecutionLock(private val jdbc: JdbcTemplate) {
    /** Returns the current attempt number. An unpicked or replaced execution cannot change job state. */
    fun lock(execution: ArkistointiJobExecution): Int = jdbc.query(
        "select consecutive_failures from scheduled_tasks where task_name = ? and task_instance = ? " +
            "and version = ? and picked = true for update",
        { row, _ -> row.getInt("consecutive_failures") + 1 },
        execution.taskName, execution.taskInstanceId, execution.version
    ).singleOrNull() ?: throw StaleArkistointiExecutionException()
}
