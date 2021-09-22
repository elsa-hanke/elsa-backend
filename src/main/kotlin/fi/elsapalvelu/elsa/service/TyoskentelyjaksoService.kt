package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO

interface TyoskentelyjaksoService {

    fun create(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO?

    fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO?

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<TyoskentelyjaksoDTO>

    fun validateByLiitettyKoejaksoon(userId: String): Triple<Boolean, Boolean, Boolean>

    fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO?

    fun delete(id: Long, userId: String)

    fun getTilastot(userId: String): TyoskentelyjaksotTilastotDTO

    fun updateLiitettyKoejaksoon(id: Long, userId: String, liitettyKoejaksoon: Boolean): TyoskentelyjaksoDTO?

    fun validatePaattymispaiva(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
    ): Boolean
}
