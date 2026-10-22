package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import java.util.UUID

fun interface ArkistointiJobProcessor {
    fun process(jobId: UUID)
}
