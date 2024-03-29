package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.UusiSuoritemerkintaDTO
import java.time.LocalDate

interface SuoritemerkintaService {

    fun create(uusiSuoritemerkintaDTO: UusiSuoritemerkintaDTO, userId: String): List<SuoritemerkintaDTO>?

    fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO?

    fun findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId: Long): List<SuoritemerkintaDTO>

    fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<SuoritemerkintaDTO>

    fun findOne(id: Long, userId: String): SuoritemerkintaDTO?

    fun delete(id: Long, userId: String)

    fun existsBySuoriteId(suoriteId: Long): Boolean
}
