package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO

interface OpintooikeusService {
    fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO>

    fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO

    fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long

    fun onOikeus(user: User): Boolean

    fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long)
}
