package fi.elsapalvelu.elsa.service.arviointi

import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikkoDTO

interface ArviointiasteikkoService {

    fun findByOpintooikeusId(opintooikeusId: Long): ArviointiasteikkoDTO?

    fun findAll(): List<ArviointiasteikkoDTO>
}
