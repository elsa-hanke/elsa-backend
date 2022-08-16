package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
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
