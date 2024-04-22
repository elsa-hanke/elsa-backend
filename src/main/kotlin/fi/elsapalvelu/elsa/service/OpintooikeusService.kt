package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaOpintooikeusDTO

interface OpintooikeusService {
    fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO>

    fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO

    fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long

    fun findOneByErikoisalaIdAndErikoistuvaLaakariKayttajaUserId(erikoisalaId: Long, userId: String): Long

    fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(userId: String, erikoisalaId: Long): Long

    fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(userId: String, erikoisalaId: Long): OpintooikeusDTO

    fun findAllByTerveyskoulutusjaksoSuorittamatta(): List<Opintooikeus>

    fun onOikeus(user: User): Boolean

    fun checkOpintooikeusKaytossaValid(user: User)

    fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long)

    fun setAktiivinenOpintooikeusKaytossa(userId: String)

    fun updateMuokkausoikeudet(userId: String, muokkausoikeudet: Boolean)

    fun updateOpintooikeudet(
        userId: String,
        opintooikeudet: List<KayttajahallintaOpintooikeusDTO>
    )
}
