package fi.elsapalvelu.elsa.scheduler

interface TriggerableJob {
    val jobName: String
    fun runJob()
}

