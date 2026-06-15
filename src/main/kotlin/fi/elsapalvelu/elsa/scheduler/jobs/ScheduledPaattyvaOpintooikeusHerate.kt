package fi.elsapalvelu.elsa.scheduler.jobs

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.OpintooikeusHerate
import fi.elsapalvelu.elsa.repository.OpintooikeusHerateRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.AbstractTriggerableJob
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class PaattyvaOikeus(
    var maaraaikainen: Boolean,
    var paattymispaiva: LocalDate?,
    var erikoistuvaLaakari: ErikoistuvaLaakari?
)

@Component
class ScheduledPaattyvaOpintooikeusHerate(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val mailService: MailService,
    private val opintooikeusHerateRepository: OpintooikeusHerateRepository
) : AbstractTriggerableJob() {

    override val jobName = "paattyvaOpintooikeusHerate"

    @Scheduled(cron = "0 0 6 * * 1", zone = "Europe/Helsinki")
    @SchedulerLock(name = "paattyvaOpintooikeusHerate", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun fetchPaattyvatOpintooikeudet() {
        runJob()
    }

    override fun runJob() {
        log.info("PaattyvaOpintooikeusHerate käynnistetty")
        val timestamp = LocalDateTime.now()
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = today.with(DayOfWeek.SUNDAY)
        val paattyvatOikeudet = listOf(
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(2), sunday.plusMonths(2))
                .mapNotNull { mapPaattyvaOikeus(it).takeIf { o -> o.maaraaikainen } },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(3), sunday.plusMonths(3))
                .mapNotNull { mapPaattyvaOikeus(it).takeIf { o -> !o.maaraaikainen } },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(4), sunday.plusMonths(4))
                .mapNotNull { mapPaattyvaOikeus(it).takeIf { o -> o.maaraaikainen } },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(6), sunday.plusMonths(6))
                .map { mapPaattyvaOikeus(it) },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusYears(1), sunday.plusYears(1))
                .mapNotNull { mapPaattyvaOikeus(it).takeIf { o -> !o.maaraaikainen } }
        )
        val totalCount = paattyvatOikeudet.sumOf { it.size }
        log.info("PaattyvaOpintooikeusHerate: löydetty $totalCount päättyvää opinto-oikeutta")
        paattyvatOikeudet.forEach {
            if (it.isNotEmpty()) it.forEach { oikeus ->
                lahetaHerate(oikeus)
            }
        }
        log.info(
            "PaattyvaOpintooikeusHerate valmis ${
                Duration.between(timestamp, LocalDateTime.now()).toSeconds()
            } sekunnissa"
        )
    }

    fun mapPaattyvaOikeus(opintoOikeus: Opintooikeus): PaattyvaOikeus {
        return PaattyvaOikeus(
            maaraaikainen = ((opintoOikeus.opintooikeudenPaattymispaiva?.year ?: 0) - (opintoOikeus.opintooikeudenMyontamispaiva?.year ?: 0)) <= 3,
            paattymispaiva = opintoOikeus.opintooikeudenPaattymispaiva,
            erikoistuvaLaakari = opintoOikeus.erikoistuvaLaakari
        )
    }

    fun lahetaHerate(paattyvaOikeus: PaattyvaOikeus) {
        try {
            val user = paattyvaOikeus.erikoistuvaLaakari?.kayttaja?.user ?: return
            log.info(
                "PaattyvaOpintooikeusHerate: lähetetään herätesähköposti käyttäjälle userId=${user.id}, " +
                    "paattymispaiva=${paattyvaOikeus.paattymispaiva}, maaraaikainen=${paattyvaOikeus.maaraaikainen}"
            )
            val properties =
                if (paattyvaOikeus.maaraaikainen) {
                    mapOf(
                        Pair(MailProperty.DATE, paattyvaOikeus.paattymispaiva.toString()),
                        Pair(
                            MailProperty.URL_PATH,
                            "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/koejakso"
                        ),
                        Pair(
                            MailProperty.SECOND_URL_PATH,
                            "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"
                        ),
                    )
                } else {
                    mapOf(
                        Pair(MailProperty.DATE, paattyvaOikeus.paattymispaiva.toString()),
                        Pair(
                            MailProperty.URL_PATH,
                            "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"
                        ),
                    )
                }
            mailService.sendEmailFromTemplate(
                user = user,
                templateName = if (paattyvaOikeus.maaraaikainen) "opintooikeusMaarakainenPaattymassa.html" else "opintooikeusPaattymassa.html",
                titleKey = "email.opintooikeuspaattymassa.title",
                properties = properties,
            )
            val opintooikeusHerate =
                opintooikeusHerateRepository.findOneByErikoistuvaLaakariKayttajaUserId(user.id!!)
                    ?: OpintooikeusHerate(
                        erikoistuvaLaakari = paattyvaOikeus.erikoistuvaLaakari
                    )
            if (paattyvaOikeus.maaraaikainen) {
                opintooikeusHerate.maaraaikainenPaattymassaHerateLahetetty = Instant.now()
            } else opintooikeusHerate.paattymassaHerateLahetetty = Instant.now()
            opintooikeusHerateRepository.save(opintooikeusHerate)
        } catch (e: Exception) {
            log.error(
                "PaattyvaOpintooikeusHerate: herätteen lähetys epäonnistui käyttäjälle " +
                    "userId=${paattyvaOikeus.erikoistuvaLaakari?.kayttaja?.user?.id}: ${e.message}",
                e
            )
        }
    }
}
