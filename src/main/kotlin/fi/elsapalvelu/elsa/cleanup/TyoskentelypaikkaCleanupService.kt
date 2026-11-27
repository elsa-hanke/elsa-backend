package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelypaikka
import org.springframework.stereotype.Component

interface TyoskentelypaikkaCleanupService {
    fun cleanTyoskentelypaikka(tyoskentelypaikka: Tyoskentelypaikka?)
}

@Component
class TyoskentelypaikkaCleanupServiceImpl : TyoskentelypaikkaCleanupService {

    override fun cleanTyoskentelypaikka(tyoskentelypaikka: Tyoskentelypaikka?) {
        tyoskentelypaikka?.nimi = ""
    }

}
