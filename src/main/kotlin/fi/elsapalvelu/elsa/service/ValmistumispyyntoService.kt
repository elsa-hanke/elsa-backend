package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoErikoistujaSaveDTO
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
        valmistumispyyntoDTO: ValmistumispyyntoErikoistujaSaveDTO
    ): ValmistumispyyntoDTO?

    fun update(
        opintooikeusId: Long,
        valmistumispyyntoDTO: ValmistumispyyntoErikoistujaSaveDTO
    ): ValmistumispyyntoDTO?
}
