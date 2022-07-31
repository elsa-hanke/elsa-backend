package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArviointiasteikkoDTO

interface ArviointiasteikkoService {

    fun findByOpintooikeusId(opintooikeusId: Long): ArviointiasteikkoDTO?

    fun findAll(): List<ArviointiasteikkoDTO>
}
