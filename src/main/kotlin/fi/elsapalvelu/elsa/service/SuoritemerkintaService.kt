package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface SuoritemerkintaService {

    fun save(suoritemerkintaDTO: SuoritemerkintaDTO): SuoritemerkintaDTO

    fun findAll(pageable: Pageable): Page<SuoritemerkintaDTO>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id: String): MutableList<SuoritemerkintaDTO>

    fun findOne(id: Long): Optional<SuoritemerkintaDTO>

    fun delete(id: Long)
}
