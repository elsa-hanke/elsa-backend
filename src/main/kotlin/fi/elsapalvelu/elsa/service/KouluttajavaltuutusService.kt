package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import java.util.Optional

interface KouluttajavaltuutusService {

    fun save(kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO

    fun findAll(): MutableList<KouluttajavaltuutusDTO>

    fun findAllValtuutettuByValtuuttajaKayttajaUserId(id: String): MutableList<KayttajaDTO>

    fun findOne(id: Long): Optional<KouluttajavaltuutusDTO>

    fun delete(id: Long)
}
