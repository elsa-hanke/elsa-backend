package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintotietoDataDTO

interface OpintotietoDataFetchingService {
    suspend fun fetchOpintotietoData(hetu: String): OpintotietoDataDTO?

    fun shouldFetchOpintotietoData(): Boolean
}
