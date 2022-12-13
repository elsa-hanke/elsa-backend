package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO

interface OpintooikeusService {
    fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO>

    fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO

    fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long

    fun findAllByTerveyskoulutusjaksoSuorittamatta(): List<Opintooikeus>

    fun onOikeus(user: User): Boolean

    fun checkOpintooikeusKaytossaValid(user: User)

    fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long)

    fun updateMuokkausoikeudet(userId: String, muokkausoikeudet: Boolean)
}
