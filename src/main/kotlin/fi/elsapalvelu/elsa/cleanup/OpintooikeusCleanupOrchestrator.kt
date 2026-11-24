package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OpintooikeusCleanupOrchestrator(
    private val opintooikeusCleanupService: OpintooikeusCleanupService,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val cleanup: CleanupOpintoOikeus,
    private val erikoistuvaLaakariCleanupService: ErikoistuvaLaakariCleanupService,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    fun clearExpiredOpintooikeusContent() {
        val expiredOpintooikeusIds = opintooikeusRepository.findOpintooikeusIdsPastViewingPeriodByMoreThanOneMonth(500)
        expiredOpintooikeusIds.forEach {
            log.info("Clearing content for expired opintooikeus with id: $it")
            clearContent(it)
            log.info("Cleared content for expired opintooikeus with id: $it")
        }
    }


    // Internal, incomplete cleanup only: no eligibility check or full-cleanup completion.
    @Transactional(rollbackFor = [EntityNotFoundException::class, Exception::class])
    fun clearContent(opintooikeusId: Long) {
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId)
            .orElseThrow { EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR) }

        val cutoffDate = LocalDate.now().minusMonths(1)
        val isBeforeCutoff = opintooikeus.viimeinenKatselupaiva?.isBefore(cutoffDate) ?: false

        // TODO lisää tarkistus että onko kyseinen opinto-oikeus jo siivottu, jos on niin ei tehdä mitään.
        if (!isBeforeCutoff) {
            log.warn("Opintooikeus with id: $opintooikeusId has no viimeinenKatselupaiva or it is not before the cutoff date, skipping cleanup.")
            return
        }
        opintooikeusCleanupService.clearOpintooikeus(opintooikeus)

        // TODO lisää tähän logiikka, millä tarkistetaan onko opinto-oikeuden käyttäjällä muita rooleja (vastuu / kouluttaja  jne),
        //  jos on niin ei poisteta erikoistuvan lääkärin tietoja.

        if( opintooikeus.erikoistuvaLaakari != null ) {
            log.info("Clearing erikoistuvaLaakari content for opintooikeus with id: $opintooikeusId")
            opintooikeus.erikoistuvaLaakari?.let { erikoistuvaLaakariCleanupService.clearErikoistuvaLaakari(it) }
        }

        throw Exception("Cleanup not fully implemented yet, throwing exception to rollback transaction.")
    }


}
