package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusWithErikoisalaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface ArvioitavaKokonaisuusService {

    fun create(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO

    fun update(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO?

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavaKokonaisuusDTO>

    fun findAllByErikoisalaIdPaged(erikoisalaId: Long?, voimassaolevat: Boolean?, pageable: Pageable): Page<ArvioitavaKokonaisuusDTO>

    fun findOne(id: Long): Optional<ArvioitavaKokonaisuusWithErikoisalaDTO>

    fun delete(id: Long)
}
