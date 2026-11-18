package fi.elsapalvelu.elsa.scheduler.jobs

import java.time.Clock
import fi.elsapalvelu.elsa.service.integration.sisu.SisuTutkintoohjelmaImportService
import fi.elsapalvelu.elsa.scheduler.AbstractTriggerableJob
import fi.elsapalvelu.elsa.service.integration.sisu.SisuTutkintoohjelmaFetchingService
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

@Component
class ScheduledSisuTutkintoohjelmaImport(
    private val sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService,
    private val sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService,
    private val clock: Clock
) : AbstractTriggerableJob() {

    override val jobName = "sisuTutkintoohjelmaImport"

    @Scheduled(cron = "0 0 3 ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "sisuTutkintoohjelmaImport", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun import() {
        runJob()
    }

    override fun runJob() {
        log.info("SisuTutkintoohjelmaImport käynnistetty")
        val aloitusaika = LocalDateTime.now(clock)
        runBlocking {
            try {
                sisuTutkintoohjelmaFetchingService.fetch()?.let {
                    sisuTutkintoohjelmaImportService.import(it)
                }
            } catch (e: Exception) {
                log.error("SisuTutkintoohjelmaImport virhe: ${e.message}", e)
            }
        }
        log.info(
            "SisuTutkintoohjelmaImport valmis ${
                Duration.between(aloitusaika, LocalDateTime.now(clock)).toSeconds()
            } sekunnissa"
        )
    }
}
