package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO

interface AsiakirjaService {

    fun create(asiakirjat: List<AsiakirjaDTO>, opintooikeusId: Long, tyoskentelyJaksoId: Long? = null): List<AsiakirjaDTO>?

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<AsiakirjaDTO>

    fun findAllByOpintooikeusIdAndTyoskentelyjaksoId(opintooikeusId: Long, tyoskentelyJaksoId: Long?): List<AsiakirjaDTO>

    fun findOne(id: Long, opintooikeusId: Long): AsiakirjaDTO?

    fun delete(id: Long, opintooikeusId: Long)

    fun delete(ids: List<Long> , opintooikeusId: Long)

    fun removeTyoskentelyjaksoReference(tyoskentelyJaksoId: Long?)

}
