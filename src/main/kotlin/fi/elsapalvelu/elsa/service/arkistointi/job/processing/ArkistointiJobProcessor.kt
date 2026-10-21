package fi.elsapalvelu.elsa.service.arkistointi.job.processing

interface ArkistointiJobProcessor {
    fun process(execution: ArkistointiJobExecution)
}
