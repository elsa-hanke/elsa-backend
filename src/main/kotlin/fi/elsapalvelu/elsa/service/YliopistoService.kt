package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.HakaYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import java.util.*

interface YliopistoService {

    fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO

    fun findAll(): List<YliopistoDTO>

    fun findOne(id: Long): Optional<YliopistoDTO>

    fun findAllHaka(): List<HakaYliopistoDTO>

    fun delete(id: Long)
}
