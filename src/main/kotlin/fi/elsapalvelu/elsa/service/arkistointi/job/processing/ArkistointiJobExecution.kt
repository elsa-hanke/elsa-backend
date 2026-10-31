package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import java.util.UUID

/** Captured once when db-scheduler claims a task; never replace it with a newer database version. */
data class ArkistointiJobExecution(val taskName: String, val taskInstanceId: String, val version: Long) {
    val jobId: UUID = try {
        UUID.fromString(taskInstanceId).also {
            require(it.toString().equals(taskInstanceId, ignoreCase = true))
        }
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Arkistointi-jobin tunniste ei ole UUID")
    }
}

/** Control flow: another execution owns this task, or it has already been acknowledged. */
internal class StaleArkistointiExecutionException : RuntimeException("Archive execution is no longer current")

/** Control flow: preserve the failed task for an explicit technical retry. */
internal class ParkArkistointiExecutionException : RuntimeException("Archive execution requires intervention")
