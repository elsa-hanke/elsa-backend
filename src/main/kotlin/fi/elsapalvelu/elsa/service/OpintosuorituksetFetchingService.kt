package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO

interface OpintosuorituksetFetchingService {

    suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetDTO?

    fun shouldFetchOpintosuoritukset(): Boolean
}
