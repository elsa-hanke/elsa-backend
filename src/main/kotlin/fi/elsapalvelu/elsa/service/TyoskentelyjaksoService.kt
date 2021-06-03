package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO

interface TyoskentelyjaksoService {

    fun save(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>? = null
    ): TyoskentelyjaksoDTO?

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<TyoskentelyjaksoDTO>

    fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO?

    fun delete(id: Long, userId: String)

    fun getTilastot(userId: String): TyoskentelyjaksotTilastotDTO

    fun updateLiitettyKoejaksoon(id: Long, userId: String, liitettyKoejaksoon: Boolean): TyoskentelyjaksoDTO?
}
