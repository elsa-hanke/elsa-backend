package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO

interface OpintoopasService {

    fun findOne(id: Long): OpintoopasDTO?

    fun findAll(): List<OpintoopasDTO>
}
