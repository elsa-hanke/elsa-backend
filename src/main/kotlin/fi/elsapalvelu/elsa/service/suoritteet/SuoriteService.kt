package fi.elsapalvelu.elsa.service.suoritteet

import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteWithErikoisalaDTO
import java.util.*

interface SuoriteService {

    fun create(suoriteDTO: SuoriteWithErikoisalaDTO): SuoriteWithErikoisalaDTO

    fun update(suoriteDTO: SuoriteWithErikoisalaDTO): SuoriteWithErikoisalaDTO?

    fun findAllOpintooikeusId(opintooikeusId: Long): List<SuoriteDTO>

    fun findOne(id: Long): Optional<SuoriteWithErikoisalaDTO>

    fun delete(id: Long)
}
