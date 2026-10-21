package fi.elsapalvelu.elsa.service.arkistointi.job

interface ArkistointiJobProcessor {
    fun process(execution: ArkistointiJobExecution)
}
