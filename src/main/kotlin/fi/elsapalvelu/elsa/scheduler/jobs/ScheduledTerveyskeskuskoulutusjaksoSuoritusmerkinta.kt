package fi.elsapalvelu.elsa.scheduler.jobs

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.AbstractTriggerableJob
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

@Component
class ScheduledTerveyskeskuskoulutusjaksoSuoritusmerkinta(
    private val opintooikeusService: OpintooikeusService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    private val mailService: MailService,
    private val kayttajaRepository: KayttajaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintosuoritusService: OpintosuoritusService
) : AbstractTriggerableJob() {

    override val jobName = "terveyskeskuskoulutusjaksoSuoritusmerkinta"

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Helsinki")
    @SchedulerLock(
        name = "terveyskeskuskoulutusjaksoSuoritusmerkinta",
        lockAtLeastFor = "5S",
        lockAtMostFor = "10M"
    )
    fun check() {
        runJob()
    }

    override fun runJob() {
        log.info("TerveyskeskuskoulutusjaksoSuoritusmerkinta käynnistetty")
        val timestamp = LocalDateTime.now()
        runBlocking {
            try {
                val opintooikeudet = opintooikeusService.findAllByTerveyskoulutusjaksoSuorittamatta()
                log.info(
                    "TerveyskeskuskoulutusjaksoSuoritusmerkinta: löydetty ${opintooikeudet.size} " +
                        "käsittelemätöntä opinto-oikeutta"
                )
                opintooikeudet.forEachIndexed { index, opintooikeus ->
                    log.info(
                        "TerveyskeskuskoulutusjaksoSuoritusmerkinta: käsitellään " +
                            "${index + 1}/${opintooikeudet.size}: opintooikeusId=${opintooikeus.id}"
                    )
                    if (opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(opintooikeus.id.required(), opintooikeus.erikoistuvaLaakari?.id.required())) {
                        opintooikeus.terveyskoulutusjaksoSuoritettu = true
                        opintooikeusRepository.save(opintooikeus)
                    } else if (terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(
                            opintooikeus.id.required()
                        )
                    ) {
                        opintooikeus.terveyskoulutusjaksoSuoritettu = true
                        opintooikeusRepository.save(opintooikeus)

                        mailService.sendEmailFromTemplate(
                            kayttajaRepository.findById(opintooikeus.erikoistuvaLaakari?.kayttaja?.id.required())
                                .get().user.required(),
                            templateName = "tkkjaksonSuoritusmerkintaHaettavissa.html",
                            titleKey = "email.tkkjaksonsuoritusmerkintahaettavissa.title",
                            properties = mapOf()
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("TerveyskeskuskoulutusjaksoSuoritusmerkinta virhe: ${e.message}", e)
            }
        }
        log.info(
            "TerveyskeskuskoulutusjaksoSuoritusmerkinta valmis ${
                Duration.between(timestamp, LocalDateTime.now()).toSeconds()
            } sekunnissa"
        )
    }
}
