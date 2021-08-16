package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO

interface SuoritemerkintaService {

    fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO?

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(userId: String): List<SuoritemerkintaDTO>

    fun findOne(id: Long, userId: String): SuoritemerkintaDTO?

    fun delete(id: Long, userId: String)
}
