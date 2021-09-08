package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO

interface KeskeytysaikaService {

    fun save(keskeytysaikaDTO: KeskeytysaikaDTO, kayttajaId: String): KeskeytysaikaDTO?

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(kayttajaId: String): List<KeskeytysaikaDTO>

    fun findOne(id: Long, kayttajaId: String): KeskeytysaikaDTO?

    fun delete(id: Long, kayttajaId: String)
}
