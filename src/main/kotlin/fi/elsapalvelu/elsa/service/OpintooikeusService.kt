package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO

interface OpintooikeusService {
    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String) : List<OpintooikeusDTO>

    fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO

    fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long

    fun onOikeus(user: User): Boolean
}
