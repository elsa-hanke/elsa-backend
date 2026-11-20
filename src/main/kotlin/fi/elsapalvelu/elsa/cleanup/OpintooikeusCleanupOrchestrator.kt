package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpintooikeusCleanupOrchestrator(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val cleanup: CleanupOpintoOikeus
) {

    // Internal, incomplete cleanup only: no eligibility check or full-cleanup completion.
    @Transactional
    fun clearContent(opintooikeusId: Long) {
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId)
            .orElseThrow { EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR) }
        koulutussuunnitelmaRepository.findOneByOpintooikeusId(requireNotNull(opintooikeus.id))
            ?.let(cleanup::clearKoulutussuunnitelmaContent)
        paivakirjamerkintaRepository.findAllByOpintooikeusId(opintooikeusId)
            .forEach(cleanup::clearPaivakirjamerkintaContent)
        seurantajaksoRepository.findByOpintooikeusId(opintooikeusId)
            .forEach(cleanup::clearSeurantajaksoContent)
    }
}
