package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PaivakirjamerkintaService {

    fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, userId: String): PaivakirjamerkintaDTO?

    fun findAll(pageable: Pageable, userId: String): Page<PaivakirjamerkintaDTO>

    fun findOne(id: Long, userId: String): PaivakirjamerkintaDTO?

    fun delete(id: Long, userId: String)

}
