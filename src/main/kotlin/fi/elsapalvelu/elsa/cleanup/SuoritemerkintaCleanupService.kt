package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface SuoritemerkintaCleanupService {
    fun cleanSuoritemerkinta(suoritemerkinta: Suoritemerkinta)
    fun cleanSuoritemerkinnat(suoritemerkinnat: Set<Suoritemerkinta>?)
}

@Component
class SuoritemerkintaCleanupServiceImpl : SuoritemerkintaCleanupService {

    @Transactional(propagation = Propagation.MANDATORY)
    override fun cleanSuoritemerkinnat(suoritemerkinnat: Set<Suoritemerkinta>?) {
        suoritemerkinnat?.forEach { cleanSuoritemerkinta(it) }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun cleanSuoritemerkinta(suoritemerkinta: Suoritemerkinta) {
        suoritemerkinta.lisatiedot = ""
    }
}
