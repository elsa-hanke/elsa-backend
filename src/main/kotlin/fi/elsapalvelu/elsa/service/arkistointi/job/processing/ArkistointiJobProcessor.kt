package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import java.util.UUID

interface ArkistointiJobProcessor {
    fun process(jobId: UUID)
}
