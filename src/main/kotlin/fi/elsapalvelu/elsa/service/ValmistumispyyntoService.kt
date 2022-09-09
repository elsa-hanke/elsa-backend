package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.dto.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.VanhentuneetSuorituksetDTO

interface ValmistumispyyntoService {
    fun findErikoisalaTyyppiByOpintooikeusId(opintooikeusId: Long): ErikoisalaTyyppi

    fun findOneByOpintooikeusId(opintooikeusId: Long): ValmistumispyyntoDTO?

    fun findSuoritustenTila(
        opintooikeusId: Long,
        erikoisalaTyyppi: ErikoisalaTyyppi
    ): VanhentuneetSuorituksetDTO

    fun create(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO

    fun update(
        opintooikeusId: Long,
        uusiValmistumispyyntoDTO: UusiValmistumispyyntoDTO
    ): ValmistumispyyntoDTO

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean
}
