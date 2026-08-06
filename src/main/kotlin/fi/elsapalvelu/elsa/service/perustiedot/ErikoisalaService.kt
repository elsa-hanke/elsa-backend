package fi.elsapalvelu.elsa.service.perustiedot

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaWithTehtavatyypitDTO
import java.util.*

interface ErikoisalaService {

    fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO

    fun findAll(): List<ErikoisalaDTO>

    fun findAllByLiittynytElsaan(): List<ErikoisalaDTO>

    fun findAllWithTehtavatyypit(): List<ErikoisalaWithTehtavatyypitDTO>

    fun findOne(id: Long): Optional<ErikoisalaDTO>

    fun delete(id: Long)

    fun findAllByIdIs(erikoisalaId: Long): List<ErikoisalaDTO>

}
