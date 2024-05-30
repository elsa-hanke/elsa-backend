package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import java.time.LocalDate

interface OpintosuoritusService {

    fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO

    fun getOpintosuorituksetByOpintooikeusIdAndTyyppiId(opintooikeusId: Long, tyyppiId: Long): OpintosuorituksetDTO

    fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long): Boolean

    fun getTerveyskoulutusjaksoSuoritetusPvm(opintooikeusId: Long): LocalDate?

}
