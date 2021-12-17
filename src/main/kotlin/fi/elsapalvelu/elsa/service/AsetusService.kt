package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.AsetusDTO

interface AsetusService {

    fun findAll(): List<AsetusDTO>

    fun findOne(id: Long): AsetusDTO?

}
