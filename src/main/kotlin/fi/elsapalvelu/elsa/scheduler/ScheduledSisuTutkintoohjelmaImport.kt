package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaFetchingService
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaImportService
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledSisuTutkintoohjelmaImport(
    private val sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService,
    private val sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "sisuTutkintoohjelmaImport", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun import() {
        runBlocking {
            try {
                sisuTutkintoohjelmaFetchingService.fetch()?.let {
                    sisuTutkintoohjelmaImportService.import(it)
                }
            } catch (e: Exception) {
                log.error("SisuTutkintoohjelmaExportJob virhe: ${e.message}")
            }
        }
    }
}
