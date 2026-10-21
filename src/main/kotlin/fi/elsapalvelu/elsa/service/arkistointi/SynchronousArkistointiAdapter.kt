package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum

interface SynchronousArkistointiAdapter {
    val yliopisto: YliopistoEnum

    fun laheta(request: SynchronousArkistointiDeliveryRequest)
}
