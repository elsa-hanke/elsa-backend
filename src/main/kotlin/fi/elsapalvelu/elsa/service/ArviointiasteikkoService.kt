package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArviointiasteikkoDTO

interface ArviointiasteikkoService {

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): ArviointiasteikkoDTO?
}
