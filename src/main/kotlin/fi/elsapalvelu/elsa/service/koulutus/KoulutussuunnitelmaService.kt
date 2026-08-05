package fi.elsapalvelu.elsa.service.koulutus

import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutussuunnitelmaDTO

interface KoulutussuunnitelmaService {
    fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        opintooikeusId: Long
    ): KoulutussuunnitelmaDTO?

    fun findOneByOpintooikeusId(opintooikeusId: Long): KoulutussuunnitelmaDTO?
}
