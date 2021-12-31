package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO

interface OpintoopasService {

    fun findAllByOpintooikeudetErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintoopasDTO>
}
