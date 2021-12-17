package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import java.util.*

interface SuoritteenKategoriaService {

    fun save(suoritteenKategoriaDTO: SuoritteenKategoriaDTO): SuoritteenKategoriaDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<SuoritteenKategoriaDTO>

    fun findOne(id: Long): Optional<SuoritteenKategoriaDTO>

    fun delete(id: Long)
}
