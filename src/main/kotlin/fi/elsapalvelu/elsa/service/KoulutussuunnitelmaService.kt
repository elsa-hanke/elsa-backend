package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO

interface KoulutussuunnitelmaService {
    fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        opintooikeusId: Long
    ): KoulutussuunnitelmaDTO?

    fun findOneByOpintooikeusId(opintooikeusId: Long): KoulutussuunnitelmaDTO?
}
