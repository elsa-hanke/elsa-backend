package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob

fun interface ArkistointiJobCreator {
    fun create(jobRequest: CreateArkistointiJobRequest): ArkistointiJob
}
