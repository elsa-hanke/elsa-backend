package fi.elsapalvelu.elsa.service.arkistointi.job.processing

fun interface ArkistointiJobProcessor {
    fun process(execution: ArkistointiJobExecution)
}
