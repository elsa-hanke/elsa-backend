package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import java.util.*

interface ArvioitavanKokonaisuudenKategoriaService {

    fun save(arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaDTO): ArvioitavanKokonaisuudenKategoriaDTO

    fun findAll(): List<ArvioitavanKokonaisuudenKategoriaDTO>

    fun findOne(id: Long): Optional<ArvioitavanKokonaisuudenKategoriaDTO>

    fun delete(id: Long)
}
