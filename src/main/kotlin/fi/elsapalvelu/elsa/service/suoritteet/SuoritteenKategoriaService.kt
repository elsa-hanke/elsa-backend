package fi.elsapalvelu.elsa.service.suoritteet

import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaWithErikoisalaDTO
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
