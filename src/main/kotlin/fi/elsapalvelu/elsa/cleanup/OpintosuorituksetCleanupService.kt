package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import org.springframework.stereotype.Component

interface  OpintosuorituksetCleanupService {

    fun cleanOpintosuoritukset(opintosuoritukset: Set<Opintosuoritus>)

    fun cleanOpintosuoritus(opintosuoritus: Opintosuoritus)
}

@Component
class OpintosuorituksetCleanupServiceImpl () : OpintosuorituksetCleanupService {

    override fun cleanOpintosuoritukset(opintosuoritukset: Set<Opintosuoritus>) {
        opintosuoritukset.forEach { cleanOpintosuoritus(it) }
    }

    override fun cleanOpintosuoritus(opintosuoritus: Opintosuoritus) {
        val s = "s t o p"

    }
}
