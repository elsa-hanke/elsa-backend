package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.LaillistamispaivaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
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

    fun getLaillistamispaiva(userId: String): LaillistamispaivaDTO?

    fun laillistamispaivaAndTodistusExists(userId: String): Boolean

    fun findOneByKayttajaUserIdWithValidOpintooikeudet(userId: String): ErikoistuvaLaakariDTO?

    fun findOneByKayttajaId(kayttajaId: Long): ErikoistuvaLaakariDTO?

    fun resendInvitation(id: Long)

    fun updateLaillistamispaiva(
        userId: String,
        laillistamispaiva: LocalDate?,
        laillistamispaivanLiitetiedosto: ByteArray?,
        laillistamispaivanLiitetiedostonNimi: String?,
        laillistamispaivanLiitetiedostonTyyppi: String?
    )

    fun updateLaakarikoulutusSuoritettuSuomiTaiBelgia(
        userId: String,
        laakarikoulutusSuoritettuSuomiTaiBelgia: Boolean?,
        laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: Boolean?
    )

    fun updateAktiivinenOpintooikeus(userId: String, opintooikeusId: Long)
}
