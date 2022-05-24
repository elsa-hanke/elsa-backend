package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface ErikoistuvaLaakariService {

    fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun save(kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun findAll(
        userId: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO>

    fun findAllFromSameYliopisto(
        userId: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO>

    fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO>

    fun delete(id: Long)

    fun findOneByKayttajaUserId(userId: String): ErikoistuvaLaakariDTO?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakariDTO?

    fun findAllForVastuuhenkilo(kayttajaId: Long): List<ErikoistuvaLaakariDTO>

    fun resendInvitation(id: Long)
}
