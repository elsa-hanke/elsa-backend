package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO

interface OpintosuorituksetFetchingService {

    suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO?

    fun shouldFetchOpintosuoritukset(): Boolean

    fun getYliopisto(): YliopistoEnum
}
