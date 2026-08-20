package fi.elsapalvelu.elsa.service.arviointi

import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
import java.util.*

interface ArvioitavanKokonaisuudenKategoriaService {

    fun save(
        arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
    ): ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO

    fun findAll(): List<ArvioitavanKokonaisuudenKategoriaDTO>

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavanKokonaisuudenKategoriaDTO>

    fun findAllByErikoisalaId(erikoisalaId: Long): List<ArvioitavanKokonaisuudenKategoriaSimpleDTO>

    fun findAllByErikoisalaIdWithKokonaisuudet(erikoisalaId: Long): List<ArvioitavanKokonaisuudenKategoriaDTO>

    fun findOne(id: Long): Optional<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO>

    fun delete(id: Long)
}
