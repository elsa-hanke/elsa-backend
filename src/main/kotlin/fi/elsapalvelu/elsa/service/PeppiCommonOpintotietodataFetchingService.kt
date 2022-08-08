package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import okhttp3.OkHttpClient

interface PeppiCommonOpintotietodataFetchingService {
    suspend fun fetchOpintotietodata(
        endpointUrl: String,
        client: OkHttpClient,
        hetu: String,
        yliopistoEnum: YliopistoEnum
    ): OpintotietodataDTO?
}
