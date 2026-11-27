package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import org.springframework.stereotype.Component


interface SuoritusarviointiCleanupService {

    fun cleanSuoritusarviointi(suoritusarviointi: Suoritusarviointi)
    fun cleanSuoritusarvioinnit(suoritusarvioinnit: Set<Suoritusarviointi>?)

}

@Component
class SuoritusarviointiCleanupServiceImpl() : SuoritusarviointiCleanupService {

    override fun cleanSuoritusarvioinnit(suoritusarvioinnit: Set<Suoritusarviointi>?) {
        suoritusarvioinnit?.forEach { cleanSuoritusarviointi(it) }
    }

    override fun cleanSuoritusarviointi(suoritusarviointi: Suoritusarviointi) {
        val s = "s t o p"
    }

}
