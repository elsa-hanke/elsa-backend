package fi.elsapalvelu.elsa.service.perustiedot

import fi.elsapalvelu.elsa.service.dto.perustiedot.AsetusDTO

interface AsetusService {

    fun findAll(): List<AsetusDTO>

    fun findOne(id: Long): AsetusDTO?

}
