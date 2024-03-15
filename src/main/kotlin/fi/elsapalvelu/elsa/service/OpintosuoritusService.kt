package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO

interface OpintosuoritusService {

    fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO

    fun getOpintosuorituksetByOpintooikeusIdAndTyyppiId(opintooikeusId: Long, tyyppiId: Long): OpintosuorituksetDTO

}
