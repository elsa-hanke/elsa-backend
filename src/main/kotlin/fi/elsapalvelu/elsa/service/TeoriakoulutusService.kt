package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO

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

    fun delete(
        id: Long,
        userId: String
    )
}
