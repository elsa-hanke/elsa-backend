package fi.elsapalvelu.elsa.service.koulutus

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetDTO

interface OpintosuoritusService {

    fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO

    fun getOpintosuorituksetByOpintooikeusIdAndTyyppi(opintooikeusId: Long, tyyppi: OpintosuoritusTyyppiEnum): OpintosuorituksetDTO

    fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long, erikoistuvaLaakariId: Long): Boolean

    fun getTerveyskoulutusjaksoSuoritusPvm(opintooikeusId: Long, erikoistuvaLaakariId: Long): LocalDate?

}
