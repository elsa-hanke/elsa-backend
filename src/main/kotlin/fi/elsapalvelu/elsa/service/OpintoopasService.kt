package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import fi.elsapalvelu.elsa.service.dto.OpintoopasSimpleDTO

interface OpintoopasService {

    fun findOne(id: Long): OpintoopasDTO?

    fun findAll(): List<OpintoopasSimpleDTO>

    fun findAllByErikoisala(erikoisalaId: Long): List<OpintoopasSimpleDTO>

    fun update(opintoopasDTO: OpintoopasDTO): OpintoopasDTO?
}
