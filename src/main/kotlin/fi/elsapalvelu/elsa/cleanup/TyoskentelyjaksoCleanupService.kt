package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface TyoskentelyjaksoCleanupService {

    fun cleanTyoskentelyjaksot(tyoskentelyjaksot: Set<Tyoskentelyjakso>)

    fun cleanTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso)
}

@Component
class TyoskentelyjaksoCleanupServiceImpl(
    private val koulutusjaksoCleanupService: KoulutusjaksoCleanupService,
    private val asiakirjaCleanupService: AsiakirjaCleanupService,
    private val keskeytysCleanupService: KeskeytysCleanupService,
    private val suoritemerkintaCleanupService: SuoritemerkintaCleanupService,
    private val suoritusarviointiCleanupService: SuoritusarviointiCleanupService,
    private val tyoskentelypaikkaCleanupService: TyoskentelypaikkaCleanupService
) : TyoskentelyjaksoCleanupService {

    private final val log = LoggerFactory.getLogger(TyoskentelyjaksoCleanupServiceImpl::class.java)

    override fun cleanTyoskentelyjaksot(tyoskentelyjaksot: Set<Tyoskentelyjakso>) {
        tyoskentelyjaksot.forEach { cleanTyoskentelyjakso(it) }
    }

    override fun cleanTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso) {
        koulutusjaksoCleanupService.cleanKoulutusjaksot(tyoskentelyjakso.koulutusjaksot)
        asiakirjaCleanupService.cleanAsiakirjaContents(tyoskentelyjakso.asiakirjat)
        keskeytysCleanupService.cleanKeskeytysajat(tyoskentelyjakso.keskeytykset)
        suoritemerkintaCleanupService.cleanSuoritemerkinnat(tyoskentelyjakso.suoritemerkinnat)
        suoritusarviointiCleanupService.cleanSuoritusarvioinnit(tyoskentelyjakso.suoritusarvioinnit)
        tyoskentelypaikkaCleanupService.cleanTyoskentelypaikka(tyoskentelyjakso.tyoskentelypaikka)
    }
}
