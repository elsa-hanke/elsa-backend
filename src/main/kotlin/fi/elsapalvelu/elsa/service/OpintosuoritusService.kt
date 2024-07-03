package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import java.time.LocalDate

interface OpintosuoritusService {

    fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO

    fun getOpintosuorituksetByOpintooikeusIdAndTyyppi(opintooikeusId: Long, tyyppi: OpintosuoritusTyyppiEnum): OpintosuorituksetDTO

    fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long, erikoistuvaLaakariId: Long): Boolean

    fun getTerveyskoulutusjaksoSuoritusPvm(opintooikeusId: Long, erikoistuvaLaakariId: Long): LocalDate?

}
