package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoDTO

interface KoulutusjaksoService {

    fun save(koulutusjaksoDTO: KoulutusjaksoDTO, opintooikeusId: Long): KoulutusjaksoDTO?

    fun findAllByKoulutussuunnitelmaOpintooikeusId(opintooikeusId: Long): List<KoulutusjaksoDTO>

    fun findOne(id: Long, opintooikeusId: Long): KoulutusjaksoDTO?

    fun findForSeurantajakso(ids: List<Long>, opintooikeusId: Long): List<KoulutusjaksoDTO>

    fun delete(id: Long, opintooikeusId: Long)

    fun removeTyoskentelyjaksoReference(tyoskentelyJaksoId: Long)
}
