package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTerveyskeskuskoulutusjaksoSuoritusmerkinta(
    private val opintooikeusService: OpintooikeusService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintosuoritusService: OpintosuoritusService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Helsinki")
    @SchedulerLock(
        name = "terveyskeskuskoulutusjaksoSuoritusmerkinta",
        lockAtLeastFor = "5S",
        lockAtMostFor = "10M"
    )
    fun check() {
        runBlocking {
            try {
                opintooikeusService.findAllByTerveyskoulutusjaksoSuorittamatta().forEach {
                    if (opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(it.id!!, it.erikoistuvaLaakari?.id!!)) {
                        it.terveyskoulutusjaksoSuoritettu = true
                        opintooikeusRepository.save(it)
                    }
                    else if (terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(
                            it.id!!
                        )
                    ) {
                        it.terveyskoulutusjaksoSuoritettu = true
                        opintooikeusRepository.save(it)

                        mailService.sendEmailFromTemplate(
                            kayttajaRepository.findById(it.erikoistuvaLaakari?.kayttaja?.id!!)
                                .get().user!!,
                            templateName = "tkkjaksonSuoritusmerkintaHaettavissa.html",
                            titleKey = "email.tkkjaksonsuoritusmerkintahaettavissa.title",
                            properties = mapOf()
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("TerveyskeskuskoulutusjaksoSuoritusmerkinta virhe: ${e.message}")
            }
        }
    }
}
