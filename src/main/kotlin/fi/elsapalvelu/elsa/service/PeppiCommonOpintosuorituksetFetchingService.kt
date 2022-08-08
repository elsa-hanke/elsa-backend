package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import okhttp3.OkHttpClient

interface PeppiCommonOpintosuorituksetFetchingService {
    suspend fun fetchOpintosuoritukset(
        endpointUrl: String,
        client: OkHttpClient,
        hetu: String,
        yliopistoEnum: YliopistoEnum
    ): OpintosuorituksetPersistenceDTO?
}
