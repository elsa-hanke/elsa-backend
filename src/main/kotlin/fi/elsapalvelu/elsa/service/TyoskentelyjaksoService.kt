package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO

interface TyoskentelyjaksoService {

    fun save(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO, userId: String): TyoskentelyjaksoDTO?

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<TyoskentelyjaksoDTO>

    fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO?

    fun delete(id: Long, userId: String)

    fun getTilastot(userId: String): TyoskentelyjaksotTilastotDTO
}
