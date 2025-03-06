package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.OpintooikeusHerate
import fi.elsapalvelu.elsa.repository.OpintooikeusHerateRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class PaattyvaOikeus (
    var maaraaikainen: Boolean,
    var paattymispaiva: LocalDate?,
    var erikoistuvaLaakari: ErikoistuvaLaakari?
)

@Component
class ScheduledPaattyvaOpintooikeusHerate (
    private val opintooikeusRepository: OpintooikeusRepository,
    private val mailService: MailService,
    private val opintooikeusHerateRepository: OpintooikeusHerateRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */1 * ? * *", zone = "Europe/Helsinki")
    @SchedulerLock(name = "paattyvaOpintooikeusHerate", lockAtLeastFor = "5S", lockAtMostFor = "10M")
    fun fetchPaattyvatOpintooikeudet() {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = today.with(DayOfWeek.SUNDAY)
        println("maanantai: $monday, sunnuntai: $sunday")
        val paattyvatOikeudet = listOf(
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(2), sunday.plusMonths(2))
                .map { mapPaattyvaOikeus(it) },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(3), sunday.plusMonths(3))
                .map { mapPaattyvaOikeus(it) },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(4), sunday.plusMonths(4))
                .map { mapPaattyvaOikeus(it) },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusMonths(6), sunday.plusMonths(6))
                .map { mapPaattyvaOikeus(it) },
            opintooikeusRepository.findAllPaattyvatByTimeFrame(monday.plusYears(1), sunday.plusYears(1))
                .map { mapPaattyvaOikeus(it) }
        )
        println(paattyvatOikeudet)
        paattyvatOikeudet.forEach {
            if (it.isNotEmpty()) it.forEach { oikeus ->
                println(oikeus)
                lahetaHerate(oikeus)
            }
        }
    }

    fun mapPaattyvaOikeus(opintoOikeus: Opintooikeus): PaattyvaOikeus {
        return PaattyvaOikeus(
            maaraaikainen = ((opintoOikeus.opintooikeudenPaattymispaiva?.year ?: 0) - (opintoOikeus.opintooikeudenMyontamispaiva?.year ?: 0)) <= 3,
            paattymispaiva = opintoOikeus.opintooikeudenPaattymispaiva,
            erikoistuvaLaakari = opintoOikeus.erikoistuvaLaakari
        )
    }

    fun lahetaHerate(paattyvaOikeus: PaattyvaOikeus) {
        val user = paattyvaOikeus.erikoistuvaLaakari?.kayttaja?.user ?: return
        println(user.email)
        println(paattyvaOikeus.maaraaikainen)
        val properties =
            if (paattyvaOikeus.maaraaikainen) {
                mapOf(
                    Pair(MailProperty.DATE, paattyvaOikeus.paattymispaiva.toString()),
                    Pair(MailProperty.URL_PATH, "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/koejakso"),
                    Pair(MailProperty.SECOND_URL_PATH, "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"),
                )
            } else {
                mapOf(
                    Pair(MailProperty.DATE, paattyvaOikeus.paattymispaiva.toString()),
                    Pair(MailProperty.URL_PATH, "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"),
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
    }
}
