package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO

interface PaivakirjamerkintaService {

    fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, opintooikeusId: Long): PaivakirjamerkintaDTO?

    fun findOne(id: Long, opintooikeusId: Long): PaivakirjamerkintaDTO?

    fun delete(id: Long, opintooikeusId: Long)

}
