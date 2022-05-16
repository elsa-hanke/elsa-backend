package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.VastuuhenkilonTehtavatyyppiDTO

interface VastuuhenkilonTehtavatyyppiService {

    fun findAll(): List<VastuuhenkilonTehtavatyyppiDTO>

    fun findOne(id: Long): VastuuhenkilonTehtavatyyppiDTO?
}
