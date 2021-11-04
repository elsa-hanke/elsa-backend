package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import java.time.LocalDate

interface TeoriakoulutusService {

    fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        todistukset: Set<AsiakirjaDTO>?,
        deletedAsiakirjaIds: Set<Int>?,
        userId: String
    ): TeoriakoulutusDTO?

    fun findAll(
        userId: String
    ): List<TeoriakoulutusDTO>

    fun findOne(
        id: Long,
        userId: String
    ): TeoriakoulutusDTO?

    fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<TeoriakoulutusDTO>

    fun delete(
        id: Long,
        userId: String
    )
}
