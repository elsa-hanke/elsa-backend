package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class ScheduledPaattyvaOpintooikeusHerate (
    private val opintooikeusRepository: OpintooikeusRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */1 * ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "paattyvaOpintooikeusHerate", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun fetchPaattyvatOpintooikeudet() {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = today.with(DayOfWeek.SUNDAY)
        println("månad: $monday, söndag: $sunday")
        val twoMonths = opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(2), sunday.plusMonths(2))
        val threeMonths = opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(3), sunday.plusMonths(3))
        val fourMonths = opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(4), sunday.plusMonths(4))
        val sixMonths = opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(6), sunday.plusMonths(6))
        val year = opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusYears(1), sunday.plusYears(1))
    }

    fun lahetaHerate() {

    }
}
