package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusByErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusWithErikoisalaDTO
import java.util.*

interface ArvioitavaKokonaisuusService {

    fun create(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO

    fun update(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO?

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavaKokonaisuusDTO>

    fun findAllByErikoisalaIds(erikoisalaIds: List<Long>): List<ArvioitavaKokonaisuusByErikoisalaDTO>

    fun findOne(id: Long): Optional<ArvioitavaKokonaisuusWithErikoisalaDTO>

    fun delete(id: Long)
}
