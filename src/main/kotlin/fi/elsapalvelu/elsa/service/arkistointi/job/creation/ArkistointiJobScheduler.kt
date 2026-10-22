package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import java.time.Instant
import java.util.UUID

interface ArkistointiJobScheduler {
    fun schedule(jobId: UUID, executionTime: Instant = Instant.now()): Boolean
}
