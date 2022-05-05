package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import java.util.*

interface KouluttajavaltuutusService {

    fun save(userId: String, kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO

    fun findAll(): List<KouluttajavaltuutusDTO>

    fun findAllValtuutettuByValtuuttajaKayttajaUserId(userId: String,): List<KouluttajavaltuutusDTO>

    fun findValtuutettuByValtuuttajaAndValtuutettu(
        userId: String,
        valtuutettuId: String,
    ): Optional<KouluttajavaltuutusDTO>

    fun findOne(id: Long): Optional<KouluttajavaltuutusDTO>

    fun delete(id: Long)
}
