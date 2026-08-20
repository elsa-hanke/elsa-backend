package fi.elsapalvelu.elsa.service.koulutus

import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusKurssikoodiDTO
import java.util.*

interface OpintosuoritusKurssikooditService {

    fun save(
        userId: String,
        opintosuoritusKurssikoodiDTO: OpintosuoritusKurssikoodiDTO
    ): OpintosuoritusKurssikoodiDTO?

    fun findAllForVirkailija(userId: String): List<OpintosuoritusKurssikoodiDTO>?

    fun findOne(id: Long, userId: String): Optional<OpintosuoritusKurssikoodiDTO>

    fun delete(id: Long, userId: String)
}
