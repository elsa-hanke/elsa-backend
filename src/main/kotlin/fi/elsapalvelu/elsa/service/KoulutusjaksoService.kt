package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoDTO

interface KoulutusjaksoService {

    fun save(koulutusjaksoDTO: KoulutusjaksoDTO, userId: String): KoulutusjaksoDTO?

    fun findAllByKoulutussuunnitelmaErikoistuvaLaakariKayttajaUserId(userId: String): List<KoulutusjaksoDTO>

    fun findOne(id: Long, userId: String): KoulutusjaksoDTO?

    fun findForSeurantajakso(ids: List<Long>, userId: String): List<KoulutusjaksoDTO>

    fun delete(id: Long, userId: String)
}
