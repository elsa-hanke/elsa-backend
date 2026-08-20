package fi.elsapalvelu.elsa.service.perustiedot

import fi.elsapalvelu.elsa.service.dto.perustiedot.VastuuhenkilonTehtavatyyppiDTO

interface VastuuhenkilonTehtavatyyppiService {

    fun findAll(): List<VastuuhenkilonTehtavatyyppiDTO>

    fun findOne(id: Long): VastuuhenkilonTehtavatyyppiDTO?
}
