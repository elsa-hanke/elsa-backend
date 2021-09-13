package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import java.util.*

interface KouluttajavaltuutusService {

    fun save(userId: String, kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO

    fun findAll(): List<KouluttajavaltuutusDTO>

    fun findAllValtuutettuByValtuuttajaKayttajaUserId(id: String): List<KouluttajavaltuutusDTO>

    fun findValtuutettuByValtuuttajaAndValtuutettu(
        valtuutettuId: String,
        valtuuttajaId: String
    ): Optional<KouluttajavaltuutusDTO>

    fun findOne(id: Long): Optional<KouluttajavaltuutusDTO>

    fun delete(id: Long)
}
