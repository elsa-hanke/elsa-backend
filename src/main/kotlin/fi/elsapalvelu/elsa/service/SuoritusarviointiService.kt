package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface SuoritusarviointiService {
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    fun findAll(pageable: Pageable): Page<SuoritusarviointiDTO>

    fun findAllByErikoistuvaLaakariId(erikoistuvaLaakariId: Long, pageable: Pageable): Page<SuoritusarviointiDTO>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String,
        pageable: Pageable
    ): Page<SuoritusarviointiDTO>

    fun findOne(id: Long): Optional<SuoritusarviointiDTO>

    fun delete(id: Long)
}
