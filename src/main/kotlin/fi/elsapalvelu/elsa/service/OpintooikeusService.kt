package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO

interface OpintooikeusService {
    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String) : List<OpintooikeusDTO>
}
