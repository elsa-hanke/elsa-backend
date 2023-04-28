package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaWithErikoisalaDTO
import java.util.*

interface SuoritteenKategoriaService {

    fun save(suoritteenKategoriaDTO: SuoritteenKategoriaWithErikoisalaDTO): SuoritteenKategoriaWithErikoisalaDTO

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<SuoritteenKategoriaDTO>

    fun findAllExpiredByOpintooikeusId(opintooikeusId: Long): List<SuoritteenKategoriaDTO>

    fun findAllByErikoisalaId(erikoisalaId: Long): List<SuoritteenKategoriaSimpleDTO>

    fun findAllByErikoisalaIdWithKokonaisuudet(erikoisalaId: Long): List<SuoritteenKategoriaDTO>

    fun findOne(id: Long): Optional<SuoritteenKategoriaWithErikoisalaDTO>

    fun delete(id: Long)
}
