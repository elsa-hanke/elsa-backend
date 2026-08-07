package fi.elsapalvelu.elsa.service.koulutus

import java.time.LocalDate
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO

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
