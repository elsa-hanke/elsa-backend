package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import java.time.LocalDate
import java.util.*

interface SuoritusarviointiService {
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    fun save(suoritusarviointiDTO: SuoritusarviointiDTO, userId: String): SuoritusarviointiDTO

    fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long
    ): List<SuoritusarviointiDTO>

    fun findOneByIdAndTyoskentelyjaksoOpintooikeusId(
        id: Long,
        opintooikeusId: Long
    ): Optional<SuoritusarviointiDTO>

    fun findOneByIdAndArvioinninAntajauserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO>

    fun findAsiakirjaBySuoritusarviointiIdAndTyoskentelyjaksoOpintooikeusId(
        id: Long,
        opintooikeusId: Long
    ): AsiakirjaDTO?

    fun findAsiakirjaBySuoritusarviointiIdAndArvioinninAntajauserId(
        id: Long,
        userId: String
    ): AsiakirjaDTO?

    fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<SuoritusarviointiDTO>

    fun delete(id: Long, opintooikeusId: Long)

    fun findAvoimetByKouluttajaOrVastuuhenkiloUserId(userId: String): List<SuoritusarviointiDTO>
}
