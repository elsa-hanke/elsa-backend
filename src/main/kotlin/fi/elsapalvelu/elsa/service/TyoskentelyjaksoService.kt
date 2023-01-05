package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO

interface TyoskentelyjaksoService {

    fun create(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO?

    fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO?

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<TyoskentelyjaksoDTO>

    fun findAllByOpintooikeusIdForKoejakso(opintooikeusId: Long): List<TyoskentelyjaksoDTO>

    fun validateByLiitettyKoejaksoon(opintooikeusId: Long): Triple<Boolean, Boolean, Boolean>

    fun findOne(id: Long, opintooikeusId: Long): TyoskentelyjaksoDTO?

    fun delete(id: Long, opintooikeusId: Long): Boolean

    fun getTilastot(opintooikeusId: Long): TyoskentelyjaksotTilastotDTO

    fun getTilastot(opintooikeus: Opintooikeus): TyoskentelyjaksotTilastotDTO

    fun updateLiitettyKoejaksoon(
        id: Long,
        opintooikeusId: Long,
        liitettyKoejaksoon: Boolean
    ): TyoskentelyjaksoDTO?

    fun getVahennettavatPaivat(tyoskentelyjaksot: List<Tyoskentelyjakso>): Map<Long, Double>

    fun validateAlkamisJaPaattymispaiva(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
    ): Boolean

    fun updateAsiakirjat(
        id: Long,
        addedFiles: Set<AsiakirjaDTO>?,
        deletedFiles: Set<Int>?
    ): TyoskentelyjaksoDTO?
}
