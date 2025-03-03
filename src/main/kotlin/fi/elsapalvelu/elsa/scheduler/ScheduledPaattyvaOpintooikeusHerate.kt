package fi.elsapalvelu.elsa.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledPaattyvaOpintooikeusHerate {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "paattyvaOpintooikeusHerate", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun fetchPaattyvatOpintooikeudet() {

    }

    fun lahetaHerate() {

    }
}
