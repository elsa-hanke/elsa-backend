package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO

interface OpintotietodataFetchingService {
    suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO?

    fun shouldFetchOpintotietodata(): Boolean

    fun getYliopisto(): YliopistoEnum
}
