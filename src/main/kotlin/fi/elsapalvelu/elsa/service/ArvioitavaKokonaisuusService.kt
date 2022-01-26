package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import java.util.*

interface ArvioitavaKokonaisuusService {

    fun save(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavaKokonaisuusDTO>

    fun findOne(id: Long): Optional<ArvioitavaKokonaisuusDTO>

    fun delete(id: Long)
}
