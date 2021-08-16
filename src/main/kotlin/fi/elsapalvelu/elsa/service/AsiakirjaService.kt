package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO

interface AsiakirjaService {

    fun create(asiakirjat: List<AsiakirjaDTO>, userId: String, tyoskentelyJaksoId: Long? = null): List<AsiakirjaDTO>?

    fun findAllByErikoistuvaLaakariUserId(userId: String): List<AsiakirjaDTO>

    fun findAllByErikoistuvaLaakariUserIdAndTyoskentelyjaksoId(userId: String, tyoskentelyJaksoId: Long?): List<AsiakirjaDTO>

    fun findOne(id: Long, userId: String): AsiakirjaDTO?

    fun delete(id: Long, userId: String)

    fun delete(ids: List<Long> , userId: String)

    fun removeTyoskentelyjaksoReference(userId: String, tyoskentelyJaksoId: Long?)

}
