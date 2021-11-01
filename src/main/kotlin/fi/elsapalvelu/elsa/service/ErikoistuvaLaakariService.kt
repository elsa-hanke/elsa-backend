package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import java.util.*

interface ErikoistuvaLaakariService {

    fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun save(kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun findAll(): List<ErikoistuvaLaakariDTO>

    fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO>

    fun delete(id: Long)

    fun findOneByKayttajaUserId(userId: String): ErikoistuvaLaakariDTO?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakariDTO?
}
