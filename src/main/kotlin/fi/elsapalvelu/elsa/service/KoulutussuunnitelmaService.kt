package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO

interface KoulutussuunnitelmaService {
    fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        userId: String
    ): KoulutussuunnitelmaDTO?

    fun findOneByErikoistuvaLaakariKayttajaUserId(userId: String): KoulutussuunnitelmaDTO?
}
