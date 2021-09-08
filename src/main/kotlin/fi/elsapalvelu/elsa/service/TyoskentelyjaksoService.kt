package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO

interface TyoskentelyjaksoService {

    fun create(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        kayttajaId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO?

    fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        kayttajaId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO?

    fun findAllByErikoistuvaLaakariKayttajaId(kayttajaId: String): List<TyoskentelyjaksoDTO>

    fun validateByLiitettyKoejaksoon(kayttajaId: String): Triple<Boolean, Boolean, Boolean>

    fun findOne(id: Long, kayttajaId: String): TyoskentelyjaksoDTO?

    fun delete(id: Long, kayttajaId: String)

    fun getTilastot(kayttajaId: String): TyoskentelyjaksotTilastotDTO

    fun updateLiitettyKoejaksoon(id: Long, kayttajaId: String, liitettyKoejaksoon: Boolean): TyoskentelyjaksoDTO?
}
