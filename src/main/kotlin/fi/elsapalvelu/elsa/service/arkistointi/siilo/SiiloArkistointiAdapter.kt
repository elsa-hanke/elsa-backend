package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiDeliveryRequest
import org.springframework.stereotype.Component

@Component
class SiiloArkistointiAdapter(
    private val helsinkiSiiloService: HelsinkiSiiloService
) : ArkistointiAdapter {
    override val yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO

    override fun laheta(request: ArkistointiDeliveryRequest) {
        helsinkiSiiloService.laheta(request.filePath, request.caseType)
    }
}
