package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO

interface SuoritemerkintaService {

    fun save(suoritemerkintaDTO: SuoritemerkintaDTO, kayttajaId: String): SuoritemerkintaDTO?

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(kayttajaId: String): List<SuoritemerkintaDTO>

    fun findOne(id: Long, kayttajaId: String): SuoritemerkintaDTO?

    fun delete(id: Long, kayttajaId: String)
}
