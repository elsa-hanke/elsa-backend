package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import org.springframework.stereotype.Component

@Component
class OpintosuorituksetCleanupService{

    fun cleanOpintosuoritukset(opintosuoritukset: Set<Opintosuoritus>) {
        opintosuoritukset.forEach { cleanOpintosuoritus(it)

        }
    }

    fun cleanOpintosuoritus(opintosuoritus: Opintosuoritus) {

    }
}
