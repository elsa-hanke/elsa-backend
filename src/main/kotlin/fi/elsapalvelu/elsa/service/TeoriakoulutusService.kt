package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import java.time.LocalDate

interface TeoriakoulutusService {

    fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        todistukset: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        opintooikeusId: Long
    ): TeoriakoulutusDTO?

    fun findAll(
        opintooikeusId: Long
    ): List<TeoriakoulutusDTO>

    fun findOne(
        id: Long,
        opintooikeusId: Long
    ): TeoriakoulutusDTO?

    fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<TeoriakoulutusDTO>

    fun delete(
        id: Long,
        opintooikeusId: Long
    )
}
