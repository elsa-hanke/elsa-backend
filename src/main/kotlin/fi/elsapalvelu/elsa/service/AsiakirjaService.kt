package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.projection.AsiakirjaItemProjection
import fi.elsapalvelu.elsa.service.projection.AsiakirjaListProjection

interface AsiakirjaService {

    fun create(asiakirjat: List<AsiakirjaDTO>, userId: String, tyoskentelyJaksoId: Long? = null): List<AsiakirjaDTO>?

    fun findAllByErikoistuvaLaakari(userId: String): MutableList<AsiakirjaListProjection>

    fun findAllByErikoistuvaLaakariAndTyoskentelyjakso(userId: String, tyoskentelyJaksoId: Long?): MutableList<AsiakirjaListProjection>

    fun findOne(id: Long, userId: String): AsiakirjaItemProjection?

    fun delete(id: Long, userId: String)

}
