package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiDeliveryRequest
import org.springframework.stereotype.Component

@Component
class LouhiArkistointiAdapter(
    private val tampereLouhiService: TampereLouhiService
) : SynchronousArkistointiAdapter {
    override val yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO

    override fun laheta(request: ArkistointiDeliveryRequest) {
        tampereLouhiService.laheta(request.filePath, request.yek)
    }
}
