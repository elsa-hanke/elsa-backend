package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO

interface OpintosuorituksetFetchingService {

    suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO?

    fun shouldFetchOpintosuoritukset(): Boolean

    fun getYliopisto(): YliopistoEnum
}
