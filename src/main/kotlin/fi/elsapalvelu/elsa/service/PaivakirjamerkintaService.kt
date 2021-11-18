package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO

interface PaivakirjamerkintaService {

    fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, userId: String): PaivakirjamerkintaDTO?

    fun findOne(id: Long, userId: String): PaivakirjamerkintaDTO?

    fun delete(id: Long, userId: String)

}
